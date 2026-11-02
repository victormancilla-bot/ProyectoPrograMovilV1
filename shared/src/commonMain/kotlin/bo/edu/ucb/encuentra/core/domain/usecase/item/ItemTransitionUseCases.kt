package bo.edu.ucb.encuentra.core.domain.usecase.item

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemTransition
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

// Un use case por transición (H05). Cada uno valida con la máquina de estados y deja su registro de auditoría.

/** DISPONIBLE → RECLAMADO: un estudiante reclama el objeto (V01). */
class ClaimItemUseCase(private val change: ChangeItemStatusUseCase) {
    suspend operator fun invoke(itemId: ItemId, note: String? = null): Result<FoundItem> =
        change(itemId, ItemTransition.CLAIM, note)
}

/** RECLAMADO → DISPONIBLE: el staff rechaza el reclamo y el objeto vuelve al catálogo (V01). */
class ReleaseItemUseCase(private val change: ChangeItemStatusUseCase) {
    suspend operator fun invoke(itemId: ItemId, note: String? = null): Result<FoundItem> =
        change(itemId, ItemTransition.RELEASE, note)
}

/** RECLAMADO → ENTREGADO: el dueño recibió el objeto (V03). */
class DeliverItemUseCase(private val change: ChangeItemStatusUseCase) {
    suspend operator fun invoke(itemId: ItemId, note: String? = null): Result<FoundItem> =
        change(itemId, ItemTransition.DELIVER, note)
}

/** DISPONIBLE → DESCARTADO: venció la retención y no se puede donar (D04). */
class DiscardItemUseCase(private val change: ChangeItemStatusUseCase) {
    suspend operator fun invoke(itemId: ItemId, note: String? = null): Result<FoundItem> =
        change(itemId, ItemTransition.DISCARD, note)
}

/** DISPONIBLE → DONADO: venció la retención y se dona (D04). */
class DonateItemUseCase(private val change: ChangeItemStatusUseCase) {
    suspend operator fun invoke(itemId: ItemId, note: String? = null): Result<FoundItem> =
        change(itemId, ItemTransition.DONATE, note)
}
