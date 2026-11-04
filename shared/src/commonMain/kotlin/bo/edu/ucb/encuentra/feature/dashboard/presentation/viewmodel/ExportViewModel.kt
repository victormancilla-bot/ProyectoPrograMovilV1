package bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.presentation.UserMessages
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DateRange
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportBuilder
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportData
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportFile
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportKind
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportRequest
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsCalculator
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.BuildExportUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveExportDataUseCase
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

data class ExportState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: ExportData = ExportData(emptyList(), emptyMap(), emptyList()),
    val kind: ExportKind = ExportKind.INVENTARIO,
    val period: MetricsPeriod = MetricsPeriod.ULTIMOS_30,
    val custom: DateRange = DateRange(null, null),
    /** Solo inventario; null = todos los estados. */
    val status: ItemStatus? = null,
    val working: Boolean = false,
    val now: Instant,
    val timeZone: TimeZone
) {
    val range: DateRange get() = DateRange.of(period, MetricsCalculator.today(now, timeZone), custom)
    val request: ExportRequest get() = ExportRequest(kind, range, status.takeIf { kind == ExportKind.INVENTARIO })
    val rows: Int get() = ExportBuilder.count(data, request, timeZone)
}

sealed class ExportEvent {
    data class OnKindSelected(val kind: ExportKind) : ExportEvent()
    data class OnPeriodSelected(val period: MetricsPeriod) : ExportEvent()
    data class OnFromPicked(val date: LocalDate) : ExportEvent()
    data class OnToPicked(val date: LocalDate) : ExportEvent()
    data class OnStatusSelected(val status: ItemStatus?) : ExportEvent()
    data object OnExport : ExportEvent()
    data object OnBack : ExportEvent()
}

sealed class ExportEffect {
    /** La pantalla escribe el archivo y abre "Compartir" (expect/actual). */
    data class Share(val file: ExportFile) : ExportEffect()
    data object NavigateBack : ExportEffect()
}

/** D03 · Exportar inventario o auditoría a CSV con filtros de fecha y estado. */
class ExportViewModel(
    observeData: ObserveExportDataUseCase,
    private val buildExport: BuildExportUseCase,
    private val messages: UserMessages,
    private val clock: Clock,
    timeZone: TimeZone
) : ViewModel() {

    private val _state = MutableStateFlow(ExportState(now = clock.now(), timeZone = timeZone))
    val state: StateFlow<ExportState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ExportEffect>()
    val effects: SharedFlow<ExportEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeData()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudieron cargar los datos") } }
                .collect { data -> _state.update { it.copy(loading = false, error = null, data = data, now = clock.now()) } }
        }
    }

    fun handleEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.OnKindSelected -> _state.update { it.copy(kind = event.kind) }
            is ExportEvent.OnPeriodSelected -> _state.update { state ->
                val custom = if (event.period == MetricsPeriod.PERSONALIZADO && state.custom.from == null && state.custom.to == null) {
                    DateRange.of(MetricsPeriod.ULTIMOS_30, MetricsCalculator.today(state.now, state.timeZone), state.custom)
                } else state.custom
                state.copy(period = event.period, custom = custom)
            }
            is ExportEvent.OnFromPicked -> _state.update { it.withCustom(event.date, it.custom.to) }
            is ExportEvent.OnToPicked -> _state.update { it.withCustom(it.custom.from, event.date) }
            is ExportEvent.OnStatusSelected -> _state.update { it.copy(status = event.status) }
            ExportEvent.OnExport -> export()
            ExportEvent.OnBack -> viewModelScope.launch { _effects.emit(ExportEffect.NavigateBack) }
        }
    }

    /** La pantalla avisa si el sistema no pudo abrir "Compartir". */
    fun onShared(success: Boolean, file: ExportFile) {
        if (success) messages.success("${file.name} listo: ${file.rows} filas.")
        else messages.error("No se pudo compartir el archivo en este dispositivo.")
    }

    private fun export() {
        val state = _state.value
        if (state.working) return
        if (state.rows == 0) {
            messages.show("No hay filas con esos filtros.")
            return
        }
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            buildExport(state.data, state.request)
                .onSuccess { _effects.emit(ExportEffect.Share(it)) }
                .onFailure { e -> messages.error(e.message ?: "No se pudo exportar") }
            _state.update { it.copy(working = false) }
        }
    }

    private fun ExportState.withCustom(from: LocalDate?, to: LocalDate?): ExportState {
        val range = if (from != null && to != null && from > to) DateRange(to, from) else DateRange(from, to)
        return copy(period = MetricsPeriod.PERSONALIZADO, custom = range)
    }
}
