package bo.edu.ucb.encuentra.core.domain.usecase.item

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStateMachine
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/** Mueve el objeto a otro lugar (oficina, locker, bodega) y lo agrega al historial de custodia (H05). Solo staff. */
class MoveCustodyUseCase(
    private val repository: FoundItemLifecycleRepository,
    private val audit: AuditRecorder,
    private val transaction: TransactionRunner,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(
        itemId: ItemId,
        place: CustodyPlace,
        detail: String = "",
        note: String? = null
    ): Result<CustodyEvent> = runCatching {
        val user = getCurrentUser() ?: throw IllegalStateException(ItemErrors.NO_SESSION)
        if (!user.role.isStaff) throw IllegalStateException(ItemErrors.NOT_STAFF)

        transaction.run {
            val item = repository.getById(itemId) ?: throw NoSuchElementException(ItemErrors.NOT_FOUND)
            if (!ItemStateMachine.isInCustody(item.status)) throw IllegalStateException(ItemErrors.NOT_IN_CUSTODY)
            val cleanDetail = detail.trim()
            if (item.custodyPlace == place && item.custodyDetail == cleanDetail) {
                throw IllegalArgumentException(ItemErrors.SAME_PLACE)
            }

            val event = CustodyEvent(
                id = ItemId.generate(),
                foundItemId = itemId,
                place = place,
                detail = cleanDetail,
                changedBy = user.id,
                at = clock.now(),
                note = note?.trim()?.ifEmpty { null }
            )
            repository.moveCustody(event)
            audit.record(
                action = AuditAction.CUSTODY_CHANGED,
                actorId = user.id,
                targetId = itemId.value,
                detail = "${item.custodyPlace.label} → ${place.label}"
            )
            event
        }
    }
}

/** Dónde estuvo guardado el objeto, del más reciente al más antiguo (detalle del inventario, H07). */
class ObserveCustodyHistoryUseCase(private val repository: FoundItemLifecycleRepository) {
    operator fun invoke(itemId: ItemId): Flow<List<CustodyEvent>> = repository.observeCustodyHistory(itemId)
}
