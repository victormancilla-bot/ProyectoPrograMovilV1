package bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveDashboardStatsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveLatestPendingClaimsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Dashboard Administrador (D01). Todo llega por Flow: se actualiza sin refrescar. */
class DashboardViewModel(
    private val observeStats: ObserveDashboardStatsUseCase,
    private val observeLatestClaims: ObserveLatestPendingClaimsUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) : ViewModel() {

    // Mockup: "Panel — La Paz" (sin el "Sede" de la etiqueta).
    private val _state = MutableStateFlow(DashboardState(campus = getCurrentUser()?.campus?.label?.removePrefix("Sede ").orEmpty()))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardEffect>()
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.OnRegisterFound -> emit(DashboardEffect.NavigateToFoundForm)
            DashboardEvent.OnSeeAllClaims -> emit(DashboardEffect.NavigateToClaims)
            DashboardEvent.OnOpenAudit -> emit(DashboardEffect.NavigateToAudit)
            is DashboardEvent.OnClaimClicked -> emit(DashboardEffect.NavigateToClaimDetail(event.claimId))
            is DashboardEvent.OnStatClicked -> emit(
                when (event.target) {
                    StatTarget.INVENTORY -> DashboardEffect.NavigateToInventory
                    StatTarget.CLAIMS -> DashboardEffect.NavigateToClaims
                    StatTarget.RETENTION -> DashboardEffect.NavigateToRetention
                }
            )
            DashboardEvent.OnRetry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            combine(observeStats(), observeLatestClaims()) { live, claims -> live to claims }
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { (live, claims) ->
                    _state.value = state.value.copy(
                        loading = false,
                        error = null,
                        stats = live.stats,
                        source = live.source,
                        latestClaims = claims,
                        now = clock.now()
                    )
                }
        }
    }

    private fun emit(effect: DashboardEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar el panel."
    }
}
