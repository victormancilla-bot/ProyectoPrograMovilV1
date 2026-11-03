package bo.edu.ucb.encuentra.feature.inventory.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.RetentionState
import bo.edu.ucb.encuentra.core.domain.model.retentionState
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionAction
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionCheck
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionErrors
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionOverview
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.RetentionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Deep link de la pantalla de Retención (lo abre la notificación del staff). */
const val RETENTION_DEEP_LINK = "ucbencuentra://retention"

/** Separa los disponibles en vencidos y por vencer (la regla está en `FoundItem.retentionState`, H07). */
internal fun overviewOf(items: List<FoundItem>, clock: Clock): RetentionOverview {
    val now = clock.now()
    val byState = items.groupBy { it.retentionState(now) }
    return RetentionOverview(
        // El que venció hace más tiempo, primero.
        expired = byState[RetentionState.VENCIDO].orEmpty().sortedBy { it.retentionUntil },
        expiring = byState[RetentionState.POR_VENCER].orEmpty().sortedBy { it.retentionUntil }
    )
}

/**
 * D04 · Chequeo diario (lo corre el PeriodicWorkRequest de WorkManager). Si hay vencidos o por vencer, avisa a
 * cada persona del staff por el canal **Retención** (N01): se guarda en su bandeja y, si tiene la sesión abierta
 * en el teléfono, sale como notificación del sistema. Una alerta por persona y por día (repetir el chequeo el mismo
 * día la actualiza, no la duplica).
 */
class CheckRetentionUseCase(
    private val repository: RetentionRepository,
    private val notifications: NotificationCenter,
    private val clock: Clock,
    private val timeZone: TimeZone
) {
    suspend operator fun invoke(): RetentionCheck {
        val overview = overviewOf(repository.availableItems(), clock)
        if (overview.isEmpty) return RetentionCheck(0, 0, 0)

        val staff = repository.staffIds()
        val now = clock.now()
        val day = now.toLocalDateTime(timeZone).date
        val (title, body) = messageFor(overview.expired.size, overview.expiring.size)
        notifications.publish(
            staff.map { userId ->
                AppNotification(
                    id = ItemId("n-retention-$userId-$day"),
                    userId = userId,
                    type = NotificationType.RETENTION,
                    title = title,
                    body = body,
                    deepLink = RETENTION_DEEP_LINK,
                    read = false,
                    createdAt = now
                )
            }
        )
        return RetentionCheck(overview.expired.size, overview.expiring.size, staff.size)
    }

    companion object {
        fun messageFor(expired: Int, expiring: Int): Pair<String, String> {
            val title = when {
                expired == 1 -> "1 objeto vencido"
                expired > 1 -> "$expired objetos vencidos"
                expiring == 1 -> "1 objeto por vencer"
                else -> "$expiring objetos por vencer"
            }
            val parts = listOfNotNull(
                expired.takeIf { it > 0 }?.let { if (it == 1) "1 vencido" else "$it vencidos" },
                expiring.takeIf { it > 0 }?.let { if (it == 1) "1 por vencer" else "$it por vencer" }
            )
            val action = if (expired > 0) "Decide si se donan o se descartan." else "Revisa si alguien los reclama a tiempo."
            return title to "En custodia hay ${parts.joinToString(" y ")}. $action"
        }
    }
}

/** Pantalla de Retención: vencidos y por vencer, en vivo. Solo staff (el resto no ve nada). */
class ObserveRetentionUseCase(
    private val repository: RetentionRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    operator fun invoke(): Flow<RetentionOverview> =
        if (getCurrentUser()?.role?.isStaff == true) {
            repository.observeAvailable().map { overviewOf(it, clock) }
        } else {
            flowOf(RetentionOverview(emptyList(), emptyList()))
        }
}

/**
 * Donar o descartar un objeto vencido. Usa las transiciones de H05 (DONATE/DISCARD), que validan con la máquina de
 * estados, cambian el estado y registran la auditoría (S05) en la misma transacción. Además exige que el objeto
 * haya vencido: no se dona algo que su dueño todavía puede reclamar.
 */
class ResolveExpiredItemUseCase(
    private val repository: RetentionRepository,
    private val donate: DonateItemUseCase,
    private val discard: DiscardItemUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(itemId: String, action: RetentionAction, note: String? = null): Result<FoundItem> {
        if (getCurrentUser()?.role?.isStaff != true) return Result.failure(IllegalStateException(RetentionErrors.NOT_STAFF))
        val item = repository.findItem(itemId) ?: return Result.failure(NoSuchElementException(RetentionErrors.NOT_FOUND))
        if (item.retentionState(clock.now()) != RetentionState.VENCIDO) {
            return Result.failure(IllegalStateException(RetentionErrors.NOT_EXPIRED))
        }
        val reason = listOfNotNull("Retención vencida", note?.trim()?.ifEmpty { null }).joinToString(": ")
        return when (action) {
            RetentionAction.DONATE -> donate(ItemId(itemId), reason)
            RetentionAction.DISCARD -> discard(ItemId(itemId), reason)
        }
    }
}
