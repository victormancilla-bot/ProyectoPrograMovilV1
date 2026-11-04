package bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DateRange
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ItemOutcome
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.Metrics
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsCalculator
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveMetricsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

data class MetricsState(
    val loading: Boolean = true,
    val error: String? = null,
    val outcomes: List<ItemOutcome> = emptyList(),
    val period: MetricsPeriod = MetricsPeriod.TODO,
    /** Desde/hasta elegidos a mano (período PERSONALIZADO). */
    val custom: DateRange = DateRange(null, null),
    val now: Instant,
    val timeZone: TimeZone
) {
    val today: LocalDate get() = MetricsCalculator.today(now, timeZone)
    val range: DateRange get() = DateRange.of(period, today, custom)
    val metrics: Metrics get() = MetricsCalculator.compute(outcomes, range, now, timeZone)
}

sealed class MetricsEvent {
    data class OnPeriodSelected(val period: MetricsPeriod) : MetricsEvent()
    data class OnFromPicked(val date: LocalDate) : MetricsEvent()
    data class OnToPicked(val date: LocalDate) : MetricsEvent()
    data object OnBack : MetricsEvent()
}

sealed class MetricsEffect {
    data object NavigateBack : MetricsEffect()
}

/** D02 · Estadísticas de la oficina (staff): retención, recuperación por categoría y destino de los objetos. */
class MetricsViewModel(
    observeMetrics: ObserveMetricsUseCase,
    private val clock: Clock,
    timeZone: TimeZone
) : ViewModel() {

    private val _state = MutableStateFlow(MetricsState(now = clock.now(), timeZone = timeZone))
    val state: StateFlow<MetricsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MetricsEffect>()
    val effects: SharedFlow<MetricsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeMetrics()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudieron calcular las métricas") } }
                .collect { outcomes -> _state.update { it.copy(loading = false, error = null, outcomes = outcomes, now = clock.now()) } }
        }
    }

    fun handleEvent(event: MetricsEvent) {
        when (event) {
            is MetricsEvent.OnPeriodSelected -> _state.update { state ->
                // Al elegir "Rango" sin fechas, se arranca con los últimos 30 días para que no quede vacío.
                val custom = if (event.period == MetricsPeriod.PERSONALIZADO && state.custom.from == null && state.custom.to == null) {
                    DateRange.of(MetricsPeriod.ULTIMOS_30, state.today, state.custom)
                } else state.custom
                state.copy(period = event.period, custom = custom)
            }
            is MetricsEvent.OnFromPicked -> _state.update { it.withCustom(from = event.date, to = it.custom.to) }
            is MetricsEvent.OnToPicked -> _state.update { it.withCustom(from = it.custom.from, to = event.date) }
            MetricsEvent.OnBack -> viewModelScope.launch { _effects.emit(MetricsEffect.NavigateBack) }
        }
    }

    /** Si "desde" queda después de "hasta", se intercambian: nunca un rango vacío por un error de dedo. */
    private fun MetricsState.withCustom(from: LocalDate?, to: LocalDate?): MetricsState {
        val range = if (from != null && to != null && from > to) DateRange(to, from) else DateRange(from, to)
        return copy(period = MetricsPeriod.PERSONALIZADO, custom = range)
    }
}
