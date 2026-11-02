package bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.CloseReportUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveMyReportsUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveReportDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Mis reportes (L05): lista y detalle en vivo desde Room; al cerrar un reporte, todo se actualiza solo. */
class MyReportsViewModel(
    private val observeReports: ObserveMyReportsUseCase,
    private val observeDetail: ObserveReportDetailUseCase,
    private val closeReport: CloseReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MyReportsState())
    val state: StateFlow<MyReportsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MyReportsEffect>()
    val effects: SharedFlow<MyReportsEffect> = _effects.asSharedFlow()

    private var listJob: Job? = null
    private var detailJob: Job? = null

    init {
        loadList()
    }

    fun handleEvent(event: MyReportsEvent) {
        when (event) {
            is MyReportsEvent.OnFilterSelected -> _state.value = state.value.copy(filter = event.filter)
            is MyReportsEvent.OnOpenReport -> openDetail(event.reportId)
            MyReportsEvent.OnBackFromDetail -> closeDetail()
            MyReportsEvent.OnCloseReportClicked -> if (state.value.canClose) {
                _state.value = state.value.copy(showCloseDialog = true, closeError = null)
            }
            MyReportsEvent.OnDismissClose -> _state.value = state.value.copy(showCloseDialog = false)
            is MyReportsEvent.OnConfirmClose -> confirmClose(event.reason)
            is MyReportsEvent.OnMatchClicked -> emit(MyReportsEffect.NavigateToMatchDetail(event.matchId.value))
            MyReportsEvent.OnSeeAllMatches -> emit(MyReportsEffect.NavigateToMatchList)
            MyReportsEvent.OnReportLost -> emit(MyReportsEffect.NavigateToReportWizard)
            MyReportsEvent.OnRetry -> loadList()
        }
    }

    private fun loadList() {
        listJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        listJob = viewModelScope.launch {
            observeReports()
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { reports -> _state.value = state.value.copy(loading = false, error = null, reports = reports) }
        }
    }

    private fun openDetail(reportId: ItemId) {
        detailJob?.cancel()
        _state.value = state.value.copy(selectedReportId = reportId, detail = null, closeError = null)
        detailJob = viewModelScope.launch {
            observeDetail(reportId)
                .catch { _state.value = state.value.copy(closeError = LOAD_ERROR) }
                .collect { detail ->
                    // Si el reporte desapareció (otro dispositivo, datos borrados) se vuelve a la lista.
                    if (detail == null) closeDetail() else _state.value = state.value.copy(detail = detail)
                }
        }
    }

    private fun closeDetail() {
        detailJob?.cancel()
        _state.value = state.value.copy(selectedReportId = null, detail = null, showCloseDialog = false, closeError = null)
    }

    private fun confirmClose(reason: CloseReason) {
        val reportId = state.value.selectedReportId ?: return
        if (state.value.closing) return
        _state.value = state.value.copy(showCloseDialog = false, closing = true, closeError = null)
        viewModelScope.launch {
            closeReport(reportId, reason)
                .onSuccess { _state.value = state.value.copy(closing = false) }
                .onFailure { e -> _state.value = state.value.copy(closing = false, closeError = e.message ?: CLOSE_ERROR) }
        }
    }

    private fun emit(effect: MyReportsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar tus reportes."
        const val CLOSE_ERROR = "No se pudo cerrar el reporte, intenta de nuevo."
    }
}
