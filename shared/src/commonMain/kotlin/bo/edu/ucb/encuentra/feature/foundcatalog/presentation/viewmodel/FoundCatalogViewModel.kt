package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogFilter
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.ObserveFoundCatalogUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoundCatalogState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<CatalogItem> = emptyList(),
    val filter: CatalogFilter = CatalogFilter()
) {
    val visible: List<CatalogItem> get() = items.filter(filter::matches)

    /** Solo las categorías y zonas que tienen objetos (no se muestran 12 zonas vacías). */
    val categories: List<Category> get() = Category.entries.filter { c -> items.any { it.category == c } }
    val zones: List<CampusZone> get() = CampusZone.entries.filter { z -> items.any { it.zone == z } }

    val hasFilters: Boolean get() = filter != CatalogFilter()
}

sealed class FoundCatalogEvent {
    data class OnCategorySelected(val category: Category?) : FoundCatalogEvent()
    data class OnZoneSelected(val zone: CampusZone?) : FoundCatalogEvent()
    data object OnClearFilters : FoundCatalogEvent()
    /** "¿Es tuyo?": se reporta y el matching lo conecta con este objeto. */
    data object OnReport : FoundCatalogEvent()
}

sealed class FoundCatalogEffect {
    data object NavigateToReport : FoundCatalogEffect()
}

/** L07 · Catálogo de objetos encontrados (vista limitada) con filtros por categoría y zona. */
class FoundCatalogViewModel(observeCatalog: ObserveFoundCatalogUseCase) : ViewModel() {

    private val _state = MutableStateFlow(FoundCatalogState())
    val state: StateFlow<FoundCatalogState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<FoundCatalogEffect>()
    val effects: SharedFlow<FoundCatalogEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeCatalog()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar el catálogo") } }
                .collect { items -> _state.update { it.copy(loading = false, error = null, items = items) } }
        }
    }

    fun handleEvent(event: FoundCatalogEvent) {
        when (event) {
            is FoundCatalogEvent.OnCategorySelected -> _state.update { it.copy(filter = it.filter.copy(category = event.category)) }
            is FoundCatalogEvent.OnZoneSelected -> _state.update { it.copy(filter = it.filter.copy(zone = event.zone)) }
            FoundCatalogEvent.OnClearFilters -> _state.update { it.copy(filter = CatalogFilter()) }
            FoundCatalogEvent.OnReport -> viewModelScope.launch { _effects.emit(FoundCatalogEffect.NavigateToReport) }
        }
    }
}
