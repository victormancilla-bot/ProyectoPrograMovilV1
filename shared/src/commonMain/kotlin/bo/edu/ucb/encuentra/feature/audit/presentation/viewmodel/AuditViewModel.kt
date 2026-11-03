package bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditFilter
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditPeriod
import bo.edu.ucb.encuentra.feature.audit.domain.usecase.ObserveAuditTrailUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** Una persona que aparece en la auditoría (para el filtro). */
data class AuditPerson(val id: String, val name: String)

data class AuditState(
    val loading: Boolean = true,
    val error: String? = null,
    val entries: List<AuditEntry> = emptyList(),
    val filter: AuditFilter = AuditFilter(),
    val now: Instant,
    val timeZone: TimeZone
) {
    val visible: List<AuditEntry> get() = filter.apply(entries, now, timeZone)

    /** Solo quienes hicieron algo, por nombre. */
    val people: List<AuditPerson>
        get() = entries.map { AuditPerson(it.actorId, it.actorName) }.distinctBy { it.id }.sortedBy { it.name }

    val hasFilters: Boolean get() = filter != AuditFilter()
}

sealed class AuditEvent {
    data class OnQueryChanged(val value: String) : AuditEvent()
    data class OnPersonSelected(val actorId: String?) : AuditEvent()
    data class OnPeriodSelected(val period: AuditPeriod) : AuditEvent()
    data object OnClearFilters : AuditEvent()
    data object OnBack : AuditEvent()
}

sealed class AuditEffect {
    data object NavigateBack : AuditEffect()
}

/** S05 · Visor de auditoría para el staff, con filtros por objeto, persona y fecha. Solo lectura. */
class AuditViewModel(
    observeAuditTrail: ObserveAuditTrailUseCase,
    private val clock: Clock,
    timeZone: TimeZone
) : ViewModel() {

    private val _state = MutableStateFlow(AuditState(now = clock.now(), timeZone = timeZone))
    val state: StateFlow<AuditState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AuditEffect>()
    val effects: SharedFlow<AuditEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAuditTrail()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar la auditoría") } }
                .collect { entries -> _state.update { it.copy(loading = false, error = null, entries = entries, now = clock.now()) } }
        }
    }

    fun handleEvent(event: AuditEvent) {
        when (event) {
            is AuditEvent.OnQueryChanged -> _state.update { it.copy(filter = it.filter.copy(query = event.value)) }
            is AuditEvent.OnPersonSelected -> _state.update { it.copy(filter = it.filter.copy(actorId = event.actorId)) }
            is AuditEvent.OnPeriodSelected -> _state.update { it.copy(filter = it.filter.copy(period = event.period), now = clock.now()) }
            AuditEvent.OnClearFilters -> _state.update { it.copy(filter = AuditFilter()) }
            AuditEvent.OnBack -> viewModelScope.launch { _effects.emit(AuditEffect.NavigateBack) }
        }
    }
}
