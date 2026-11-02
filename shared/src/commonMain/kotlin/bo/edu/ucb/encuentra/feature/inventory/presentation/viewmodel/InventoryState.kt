package bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryFilter
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryItemDetail
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventorySearch

/** Borrador del diálogo "Editar custodia". */
data class CustodyDraft(val place: CustodyPlace, val detail: String = "", val note: String = "")

/** Inventario de objetos (H07, mockup "Inventario"): lista con búsqueda y chips + detalle en la misma pantalla. */
data class InventoryState(
    val loading: Boolean = true,
    val error: String? = null,
    /** "UCB Sede La Paz" del header. */
    val campus: String = "",
    val entries: List<InventoryEntry> = emptyList(),
    val filter: InventoryFilter = InventoryFilter.TODOS,
    val query: String = "",
    /** Objeto abierto; null = lista. */
    val selectedId: String? = null,
    val detail: InventoryItemDetail? = null,
    val custodyDraft: CustodyDraft? = null,
    val working: Boolean = false,
    val actionError: String? = null,
    val info: String? = null
) {
    val visible: List<InventoryEntry> get() = InventorySearch.apply(entries, filter, query)
}

sealed class InventoryEvent {
    data class OnQueryChanged(val query: String) : InventoryEvent()
    data class OnFilterSelected(val filter: InventoryFilter) : InventoryEvent()
    data class OnItemClicked(val itemId: String) : InventoryEvent()
    data object OnCloseDetail : InventoryEvent()
    data object OnRetry : InventoryEvent()
    data object OnRegisterFound : InventoryEvent()

    // Editar custodia (H05: MoveCustodyUseCase)
    data object OnEditCustody : InventoryEvent()
    data class OnCustodyPlaceSelected(val place: CustodyPlace) : InventoryEvent()
    data class OnCustodyDetailChanged(val detail: String) : InventoryEvent()
    data class OnCustodyNoteChanged(val note: String) : InventoryEvent()
    data object OnSaveCustody : InventoryEvent()
    data object OnDismissCustody : InventoryEvent()
}

sealed class InventoryEffect {
    data object NavigateToFoundForm : InventoryEffect()
}
