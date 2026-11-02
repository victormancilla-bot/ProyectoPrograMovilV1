package bo.edu.ucb.encuentra.feature.delivery.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.ClaimNotifier
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Checklist de verificación física del mockup "Confirmación de Entrega": ambos son obligatorios. */
data class PhysicalChecks(
    /** "Confirmo que he verificado la identidad del estudiante usando su Carnet Universitario físico…" */
    val identityVerified: Boolean = false,
    /** "El estudiante ha recibido el objeto listado arriba en las condiciones descritas originalmente." */
    val itemReceived: Boolean = false
) {
    val complete: Boolean get() = identityVerified && itemReceived
}

object DeliveryErrors {
    const val NOT_APPROVED = "Solo se puede entregar un reclamo aprobado."
    const val PAST_DATE = "La fecha y hora de entrega no pueden estar en el pasado."
    const val CHECKS_REQUIRED = "Completa la verificación física antes de confirmar la entrega."
}

/**
 * "Fecha y hora programada" (V04): el staff agenda cuándo el estudiante recoge el objeto. Solo para reclamos
 * aprobados y en el futuro; se puede reprogramar. Queda en la auditoría (S05) y se avisa al estudiante (N01).
 */
class ScheduleDeliveryUseCase(
    private val claims: ClaimRepository,
    private val items: FoundItemLifecycleRepository,
    private val audit: AuditRecorder,
    private val transaction: TransactionRunner,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val notifier: ClaimNotifier
) {
    suspend operator fun invoke(claimId: ItemId, at: Instant): Result<Claim> = runCatching {
        val user = getCurrentUser() ?: throw ClaimRequestException(ItemErrors.NO_SESSION)
        if (!user.role.isStaff) throw ClaimRequestException(ItemErrors.NOT_STAFF)
        // Un minuto de tolerancia: elegir "ahora" en el reloj no debe fallar por los segundos.
        if (at < clock.now() - TOLERANCE) throw ClaimRequestException(DeliveryErrors.PAST_DATE)
        transaction.run {
            val claim = claims.getById(claimId) ?: throw ClaimRequestException(ClaimErrors.CLAIM_NOT_FOUND)
            if (claim.status != ClaimStatus.APROBADO) throw ClaimRequestException(DeliveryErrors.NOT_APPROVED)
            val scheduled = claim.copy(scheduledDeliveryAt = at)
            claims.update(scheduled)
            val local = at.toLocalDateTime(timeZone)
            audit.record(
                AuditAction.DELIVERY_SCHEDULED, user.id, claim.id.value,
                "Reclamo #${claim.number} · entrega programada para ${local.date} ${local.time}"
            )
            notifier.onDeliveryScheduled(scheduled, items.getById(claim.foundItemId)?.title ?: "tu objeto")
            scheduled
        }
    }

    private companion object {
        val TOLERANCE = 1.minutes
    }
}

/**
 * "Confirmar entrega" (V04): exige la verificación física completa y aplica DELIVER (V02): reclamo y objeto
 * ENTREGADO, reporte "Recuperado", otros reclamos abiertos rechazados, auditoría (S05) y aviso al estudiante (N01).
 */
class ConfirmDeliveryUseCase(private val deliver: DeliverClaimUseCase) {
    suspend operator fun invoke(claimId: ItemId, checks: PhysicalChecks): Result<Claim> {
        if (!checks.complete) return Result.failure(ClaimRequestException(DeliveryErrors.CHECKS_REQUIRED))
        return deliver(claimId, note = NOTE)
    }

    companion object {
        const val NOTE = "Identidad verificada con carnet universitario · objeto recibido conforme"
    }
}
