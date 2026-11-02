package bo.edu.ucb.encuentra.core.domain.usecase.item

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStateMachine
import bo.edu.ucb.encuentra.core.domain.model.ItemTransition
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/**
 * Aplica una [ItemTransition] validando con [ItemStateMachine]: cambia el estado y lo audita en la misma transacción.
 * Las features usan los use cases de cada transición (ClaimItemUseCase, DeliverItemUseCase...), no este directamente.
 */
class ChangeItemStatusUseCase(
    private val repository: FoundItemLifecycleRepository,
    private val audit: AuditRecorder,
    private val transaction: TransactionRunner,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(itemId: ItemId, transition: ItemTransition, note: String? = null): Result<FoundItem> =
        runCatching {
            val user = getCurrentUser() ?: throw IllegalStateException(ItemErrors.NO_SESSION)
            // Reclamar lo hace el estudiante (V01); el resto solo el staff.
            if (transition != ItemTransition.CLAIM && !user.role.isStaff) throw IllegalStateException(ItemErrors.NOT_STAFF)

            transaction.run {
                // Se lee dentro de la transacción: nadie cambia el estado entre la validación y el update.
                val item = repository.getById(itemId) ?: throw NoSuchElementException(ItemErrors.NOT_FOUND)
                val newStatus = ItemStateMachine.apply(item.status, transition)
                repository.updateStatus(itemId, newStatus)
                audit.record(
                    action = transition.auditAction,
                    actorId = user.id,
                    targetId = itemId.value,
                    detail = listOfNotNull("${item.status.label} → ${newStatus.label}", note?.trim()?.ifEmpty { null })
                        .joinToString(" · ")
                )
                item.copy(status = newStatus)
            }
        }
}

/** Mensajes de error comunes a los use cases del objeto. */
object ItemErrors {
    const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
    const val NOT_STAFF = "Solo el staff puede hacer este cambio"
    const val NOT_FOUND = "El objeto no existe"
    const val NOT_IN_CUSTODY = "El objeto ya salió de custodia"
    const val SAME_PLACE = "El objeto ya está en ese lugar"
}
