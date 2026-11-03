package bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ClearReadAlertsUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAlertReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAllAlertsReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveAlertsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Bandeja de Alertas (N04): lista de AppNotification, leídas y no leídas. Tocar una la marca como leída y abre
 * su deep link (el match o el reclamo, con la misma navegación que la notificación del sistema de N01).
 */
class AlertsViewModel(
    private val observeAlerts: ObserveAlertsUseCase,
    private val markRead: MarkAlertReadUseCase,
    private val markAllRead: MarkAllAlertsReadUseCase,
    private val clearRead: ClearReadAlertsUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsState(now = clock.now()))
    val state: StateFlow<AlertsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AlertsEffect>()
    val effects: SharedFlow<AlertsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAlerts()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudieron cargar las alertas") } }
                .collect { alerts ->
                    _state.update { it.copy(loading = false, error = null, alerts = alerts, now = clock.now()) }
                }
        }
    }

    fun handleEvent(event: AlertsEvent) {
        when (event) {
            is AlertsEvent.OnFilterChanged -> _state.update { it.copy(filter = event.filter) }
            is AlertsEvent.OnOpen -> viewModelScope.launch {
                markRead(event.alert)
                event.alert.deepLink?.let { _effects.emit(AlertsEffect.OpenLink(it)) }
            }
            is AlertsEvent.OnMarkRead -> viewModelScope.launch { markRead(event.alert) }
            AlertsEvent.OnMarkAllRead -> viewModelScope.launch { markAllRead() }
            AlertsEvent.OnRequestClear -> _state.update { it.copy(confirmClear = true) }
            AlertsEvent.OnDismissClear -> _state.update { it.copy(confirmClear = false) }
            AlertsEvent.OnConfirmClear -> {
                _state.update { it.copy(confirmClear = false) }
                viewModelScope.launch { clearRead() }
            }
        }
    }
}

enum class AlertsFilter(val label: String) { TODAS("Todas"), NO_LEIDAS("No leídas") }

data class AlertsState(
    val loading: Boolean = true,
    val error: String? = null,
    val alerts: List<AppNotification> = emptyList(),
    val filter: AlertsFilter = AlertsFilter.TODAS,
    val confirmClear: Boolean = false,
    val now: kotlin.time.Instant
) {
    val unreadCount: Int get() = alerts.count { !it.read }
    val readCount: Int get() = alerts.size - unreadCount

    /** Nuevas (no leídas) primero, cada grupo de la más reciente a la más antigua. */
    val unread: List<AppNotification> get() = alerts.filter { !it.read }
    val earlier: List<AppNotification> get() = if (filter == AlertsFilter.NO_LEIDAS) emptyList() else alerts.filter { it.read }

    fun countFor(filter: AlertsFilter) = if (filter == AlertsFilter.TODAS) alerts.size else unreadCount
}

sealed class AlertsEvent {
    data class OnFilterChanged(val filter: AlertsFilter) : AlertsEvent()
    data class OnOpen(val alert: AppNotification) : AlertsEvent()
    data class OnMarkRead(val alert: AppNotification) : AlertsEvent()
    data object OnMarkAllRead : AlertsEvent()
    data object OnRequestClear : AlertsEvent()
    data object OnConfirmClear : AlertsEvent()
    data object OnDismissClear : AlertsEvent()
}

sealed class AlertsEffect {
    /** Deep link de F05 (`ucbencuentra://match/{id}`): lo resuelve la navegación. */
    data class OpenLink(val uri: String) : AlertsEffect()
}
