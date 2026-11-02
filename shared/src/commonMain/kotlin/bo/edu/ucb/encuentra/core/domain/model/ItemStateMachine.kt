package bo.edu.ucb.encuentra.core.domain.model

/**
 * Transiciones de un objeto encontrado (H05). Cada una dice a qué estado lleva y qué se audita (S05).
 *
 * ```
 * DISPONIBLE ──CLAIM──▶ RECLAMADO ──DELIVER──▶ ENTREGADO
 *     ▲                    │
 *     └──────RELEASE───────┘   (reclamo rechazado, V01)
 * DISPONIBLE ──DISCARD──▶ DESCARTADO   (retención vencida, D04)
 * DISPONIBLE ──DONATE───▶ DONADO       (retención vencida, D04)
 * ```
 */
enum class ItemTransition(val target: ItemStatus, val auditAction: AuditAction, val label: String) {
    CLAIM(ItemStatus.RECLAMADO, AuditAction.CLAIM_CREATED, "reclamar"),
    RELEASE(ItemStatus.DISPONIBLE, AuditAction.CLAIM_REJECTED, "liberar"),
    DELIVER(ItemStatus.ENTREGADO, AuditAction.ITEM_DELIVERED, "entregar"),
    DISCARD(ItemStatus.DESCARTADO, AuditAction.ITEM_DISCARDED, "descartar"),
    DONATE(ItemStatus.DONADO, AuditAction.ITEM_DONATED, "donar")
}

/** Se intentó una transición que la máquina de estados no permite. */
class InvalidTransitionException(val from: ItemStatus, val transition: ItemTransition) :
    IllegalStateException("No se puede ${transition.label} un objeto en estado \"${from.label}\"")

object ItemStateMachine {

    private val allowed: Map<ItemStatus, Set<ItemTransition>> = mapOf(
        ItemStatus.DISPONIBLE to setOf(ItemTransition.CLAIM, ItemTransition.DISCARD, ItemTransition.DONATE),
        ItemStatus.RECLAMADO to setOf(ItemTransition.DELIVER, ItemTransition.RELEASE),
        // Estados finales: el objeto ya salió de custodia.
        ItemStatus.ENTREGADO to emptySet(),
        ItemStatus.DESCARTADO to emptySet(),
        ItemStatus.DONADO to emptySet()
    )

    fun transitionsFrom(status: ItemStatus): Set<ItemTransition> = allowed.getValue(status)

    fun canApply(from: ItemStatus, transition: ItemTransition): Boolean = transition in transitionsFrom(from)

    /** Estado al que lleva [transition], o [InvalidTransitionException] si no está permitida. */
    fun apply(from: ItemStatus, transition: ItemTransition): ItemStatus {
        if (!canApply(from, transition)) throw InvalidTransitionException(from, transition)
        return transition.target
    }

    fun isFinal(status: ItemStatus): Boolean = transitionsFrom(status).isEmpty()

    /** Solo se puede mover de lugar lo que sigue físicamente en custodia (oficina, locker, bodega). */
    fun isInCustody(status: ItemStatus): Boolean = status == ItemStatus.DISPONIBLE || status == ItemStatus.RECLAMADO
}
