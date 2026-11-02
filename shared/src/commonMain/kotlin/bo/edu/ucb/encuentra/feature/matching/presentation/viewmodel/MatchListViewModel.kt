package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMyMatchesUseCase
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

/** Posibles coincidencias (M04). La lista se actualiza sola cuando cambian los matches en Room. */
class MatchListViewModel(
    private val observeMatches: ObserveMyMatchesUseCase,
    private val rejectMatch: RejectMatchUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MatchListState())
    val state: StateFlow<MatchListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MatchListEffect>()
    val effects: SharedFlow<MatchListEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: MatchListEvent) {
        when (event) {
            is MatchListEvent.OnReportSelected -> _state.value = state.value.copy(selectedReportId = event.reportId)
            is MatchListEvent.OnMatchClicked -> emit(MatchListEffect.NavigateToMatchDetail(event.matchId.value))
            MatchListEvent.OnModifyCriteria -> emit(MatchListEffect.NavigateToMyReports)
            MatchListEvent.OnReportLost -> emit(MatchListEffect.NavigateToReportWizard)
            MatchListEvent.OnRetry -> load()
            is MatchListEvent.OnRejectMatch -> _state.value = state.value.copy(pendingReject = event.matchId, rejectError = null)
            MatchListEvent.OnDismissReject -> _state.value = state.value.copy(pendingReject = null)
            MatchListEvent.OnConfirmReject -> confirmReject()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeMatches()
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { groups ->
                    val current = state.value
                    // Se mantiene el chip elegido mientras ese reporte siga teniendo candidatos.
                    val selected = current.selectedReportId?.takeIf { id -> groups.any { it.report.id == id } }
                    _state.value = current.copy(
                        loading = false,
                        error = null,
                        groups = groups,
                        selectedReportId = selected ?: groups.firstOrNull()?.report?.id
                    )
                }
        }
    }

    private fun confirmReject() {
        val matchId = state.value.pendingReject ?: return
        _state.value = state.value.copy(pendingReject = null)
        // La lista se actualiza sola desde Room: la tarjeta desaparece y puede entrar el siguiente candidato.
        viewModelScope.launch {
            rejectMatch(matchId).onFailure { _state.value = state.value.copy(rejectError = REJECT_ERROR) }
        }
    }

    private fun emit(effect: MatchListEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar tus coincidencias."
        const val REJECT_ERROR = "No se pudo marcar como \"No es mío\", intenta de nuevo."
    }
}
