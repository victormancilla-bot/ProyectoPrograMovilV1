package bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveOpenClaimsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Reclamos pendientes o en verificación, en vivo desde Room (primero los que ya respondieron). */
class ClaimsViewModel(private val observeOpen: ObserveOpenClaimsUseCase) : ViewModel() {

    private val _state = MutableStateFlow(ClaimsState())
    val state: StateFlow<ClaimsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ClaimsEffect>()
    val effects: SharedFlow<ClaimsEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: ClaimsEvent) {
        when (event) {
            is ClaimsEvent.OnClaimClicked -> viewModelScope.launch { _effects.emit(ClaimsEffect.NavigateToDetail(event.claimId)) }
            ClaimsEvent.OnRetry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeOpen()
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { claims -> _state.value = state.value.copy(loading = false, claims = claims) }
        }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar los reclamos."
    }
}
