package bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.MoveCustodyUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryItemUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Inventario (H07). La lista y el detalle se actualizan solos desde Room (p. ej. tras editar la custodia). */
class InventoryViewModel(
    private val observeInventory: ObserveInventoryUseCase,
    private val observeItem: ObserveInventoryItemUseCase,
    private val moveCustody: MoveCustodyUseCase,
    getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState(campus = getCurrentUser()?.campus?.label.orEmpty()))
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<InventoryEffect>()
    val effects: SharedFlow<InventoryEffect> = _effects.asSharedFlow()

    private var listJob: Job? = null
    private var detailJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: InventoryEvent) {
        val current = state.value
        when (event) {
            is InventoryEvent.OnQueryChanged -> _state.value = current.copy(query = event.query)
            is InventoryEvent.OnFilterSelected -> _state.value = current.copy(filter = event.filter)
            is InventoryEvent.OnItemClicked -> openDetail(event.itemId)
            InventoryEvent.OnCloseDetail -> closeDetail()
            InventoryEvent.OnRetry -> load()
            InventoryEvent.OnRegisterFound -> viewModelScope.launch { _effects.emit(InventoryEffect.NavigateToFoundForm) }
            InventoryEvent.OnEditCustody -> current.detail?.takeIf { it.canEditCustody }?.let { detail ->
                val item = detail.entry.item
                _state.value = current.copy(
                    custodyDraft = CustodyDraft(item.custodyPlace, item.custodyDetail),
                    actionError = null,
                    info = null
                )
            }
            is InventoryEvent.OnCustodyPlaceSelected -> updateDraft { it.copy(place = event.place) }
            is InventoryEvent.OnCustodyDetailChanged -> updateDraft { it.copy(detail = event.detail) }
            is InventoryEvent.OnCustodyNoteChanged -> updateDraft { it.copy(note = event.note) }
            InventoryEvent.OnDismissCustody -> _state.value = current.copy(custodyDraft = null, actionError = null)
            InventoryEvent.OnSaveCustody -> saveCustody()
        }
    }

    private fun load() {
        listJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        listJob = viewModelScope.launch {
            observeInventory()
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { entries -> _state.value = state.value.copy(loading = false, entries = entries) }
        }
    }

    private fun openDetail(itemId: String) {
        detailJob?.cancel()
        _state.value = state.value.copy(selectedId = itemId, detail = null, info = null, actionError = null)
        detailJob = viewModelScope.launch {
            observeItem(ItemId(itemId)).collect { detail -> _state.value = state.value.copy(detail = detail) }
        }
    }

    private fun closeDetail() {
        detailJob?.cancel()
        _state.value = state.value.copy(selectedId = null, detail = null, custodyDraft = null, info = null, actionError = null)
    }

    private fun updateDraft(change: (CustodyDraft) -> CustodyDraft) {
        state.value.custodyDraft?.let { _state.value = state.value.copy(custodyDraft = change(it), actionError = null) }
    }

    private fun saveCustody() {
        val current = state.value
        val itemId = current.selectedId ?: return
        val draft = current.custodyDraft ?: return
        if (current.working) return
        _state.value = current.copy(working = true, actionError = null)
        viewModelScope.launch {
            moveCustody(ItemId(itemId), draft.place, draft.detail, draft.note)
                // El detalle y el historial se actualizan solos desde Room.
                .onSuccess { _state.value = state.value.copy(working = false, custodyDraft = null, info = CUSTODY_SAVED) }
                .onFailure { _state.value = state.value.copy(working = false, actionError = it.message ?: SAVE_ERROR) }
        }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar el inventario."
        const val SAVE_ERROR = "No se pudo guardar la custodia, intenta de nuevo."
        const val CUSTODY_SAVED = "Custodia actualizada y registrada en el historial."
    }
}
