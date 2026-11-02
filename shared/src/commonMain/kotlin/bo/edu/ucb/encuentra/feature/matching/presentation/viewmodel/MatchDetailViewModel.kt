package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ClaimMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMatchDetailUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RejectMatchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Detalle de match (M05). Se actualiza solo desde Room (p. ej. si el staff confirma la coincidencia). */
class MatchDetailViewModel(
    private val matchId: ItemId,
    private val observeDetail: ObserveMatchDetailUseCase,
    private val claimMatch: ClaimMatchUseCase,
    private val rejectMatch: RejectMatchUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MatchDetailEffect>()
    val effects: SharedFlow<MatchDetailEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: MatchDetailEvent) {
        when (event) {
            MatchDetailEvent.OnBack -> emit(MatchDetailEffect.NavigateBack)
            MatchDetailEvent.OnRetry -> load()
            MatchDetailEvent.OnClaim -> claim()
            MatchDetailEvent.OnReject -> if (state.value.canAct) {
                _state.value = state.value.copy(confirmingReject = true, actionError = null)
            }
            MatchDetailEvent.OnDismissReject -> _state.value = state.value.copy(confirmingReject = false)
            MatchDetailEvent.OnConfirmReject -> confirmReject()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeDetail(matchId)
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { detail -> _state.value = state.value.copy(loading = false, error = null, detail = detail) }
        }
    }

    private fun claim() {
        val current = state.value
        if (!current.canAct || current.working) return
        _state.value = current.copy(working = true, actionError = null)
        viewModelScope.launch {
            claimMatch(matchId)
                .onSuccess { number -> _state.value = state.value.copy(working = false, claimNumber = number) }
                .onFailure { error ->
                    // Los errores de V02 ("Ya reclamaste este objeto...") se muestran tal cual; el resto, genérico.
                    val message = if (error is ClaimRequestException) error.message else CLAIM_ERROR
                    _state.value = state.value.copy(working = false, actionError = message)
                }
        }
    }

    private fun confirmReject() {
        if (state.value.working) return
        _state.value = state.value.copy(confirmingReject = false, working = true)
        viewModelScope.launch {
            rejectMatch(matchId)
                .onSuccess {
                    _state.value = state.value.copy(working = false)
                    _effects.emit(MatchDetailEffect.NavigateBack)
                }
                .onFailure { _state.value = state.value.copy(working = false, actionError = REJECT_ERROR) }
        }
    }

    private fun emit(effect: MatchDetailEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar esta coincidencia."
        const val CLAIM_ERROR = "No se pudo enviar el reclamo, intenta de nuevo."
        const val REJECT_ERROR = "No se pudo marcar como \"No es mío\", intenta de nuevo."
    }
}
