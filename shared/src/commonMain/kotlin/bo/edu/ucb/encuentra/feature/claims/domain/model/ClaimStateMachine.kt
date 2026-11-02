package bo.edu.ucb.encuentra.feature.claims.domain.model

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException

/**
 * Transiciones de un reclamo (V02). Cada una dice a qué estado lleva, qué se audita y quién la hace.
 *
 * ```
 * PENDIENTE ──SUBMIT_ANSWERS──▶ EN_VERIFICACION ──APPROVE──▶ APROBADO ──DELIVER──▶ ENTREGADO
 *     │                              │
 *     └──────────REJECT──────────────┴──────▶ RECHAZADO
 * ```
 * Responder las preguntas de control lo hace quien reclama (V01); el resto, el staff (V03 / V04).
 */
enum class ClaimTransition(val target: ClaimStatus, val auditAction: AuditAction, val label: String, val byStaff: Boolean) {
    SUBMIT_ANSWERS(ClaimStatus.EN_VERIFICACION, AuditAction.CLAIM_ANSWERED, "responder", byStaff = false),
    APPROVE(ClaimStatus.APROBADO, AuditAction.CLAIM_APPROVED, "aprobar", byStaff = true),
    REJECT(ClaimStatus.RECHAZADO, AuditAction.CLAIM_REJECTED, "rechazar", byStaff = true),
    DELIVER(ClaimStatus.ENTREGADO, AuditAction.ITEM_DELIVERED, "entregar", byStaff = true)
}

/** Se intentó una transición que la máquina de estados del reclamo no permite. */
class InvalidClaimTransitionException(val from: ClaimStatus, val transition: ClaimTransition) :
    ClaimRequestException("No se puede ${transition.label} un reclamo en estado \"${from.label}\".")

object ClaimStateMachine {

    private val allowed: Map<ClaimStatus, Set<ClaimTransition>> = mapOf(
        ClaimStatus.PENDIENTE to setOf(ClaimTransition.SUBMIT_ANSWERS, ClaimTransition.REJECT),
        ClaimStatus.EN_VERIFICACION to setOf(ClaimTransition.APPROVE, ClaimTransition.REJECT),
        ClaimStatus.APROBADO to setOf(ClaimTransition.DELIVER),
        // Estados finales.
        ClaimStatus.RECHAZADO to emptySet(),
        ClaimStatus.ENTREGADO to emptySet()
    )

    fun transitionsFrom(status: ClaimStatus): Set<ClaimTransition> = allowed.getValue(status)

    fun canApply(from: ClaimStatus, transition: ClaimTransition): Boolean = transition in transitionsFrom(from)

    /** Estado al que lleva [transition], o [InvalidClaimTransitionException] si no está permitida. */
    fun apply(from: ClaimStatus, transition: ClaimTransition): ClaimStatus {
        if (!canApply(from, transition)) throw InvalidClaimTransitionException(from, transition)
        return transition.target
    }

    fun isFinal(status: ClaimStatus): Boolean = transitionsFrom(status).isEmpty()

    /** Reclamo en curso: mantiene el objeto RECLAMADO y el reporte en verificación. */
    fun isOpen(status: ClaimStatus): Boolean = !isFinal(status)
}

/** Mensajes de error de los reclamos (se muestran tal cual). */
object ClaimErrors {
    const val MATCH_NOT_FOUND = "La coincidencia ya no existe."
    const val CLAIM_NOT_FOUND = "El reclamo no existe."
    const val NOT_YOUR_REPORT = "Solo el dueño del reporte puede reclamar este objeto."
    const val NOT_YOUR_CLAIM = "Solo quien hizo el reclamo puede responder sus preguntas."
    const val MATCH_CLOSED = "Esta coincidencia ya no se puede reclamar."
    const val ITEM_UNAVAILABLE = "Este objeto ya no está en custodia."
    const val ANSWERS_REQUIRED = "Responde todas las preguntas de control."
    const val ALREADY_APPROVED = "Ya hay otro reclamo aprobado para este objeto."
    const val REASON_REQUIRED = "Escribe el motivo de la decisión: queda en la auditoría."
}
