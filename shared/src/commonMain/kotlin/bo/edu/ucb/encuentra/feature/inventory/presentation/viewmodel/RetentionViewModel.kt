package bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.presentation.UserMessages
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionAction
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionOverview
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.CheckRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ResolveExpiredItemUseCase
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
import kotlin.time.Instant

/** Donar o descartar espera confirmación (no se puede deshacer). */
data class PendingResolution(val item: FoundItem, val action: RetentionAction)

data class RetentionState(
    val loading: Boolean = true,
    val error: String? = null,
    val overview: RetentionOverview = RetentionOverview(emptyList(), emptyList()),
    val now: Instant,
    val pending: PendingResolution? = null,
    val note: String = "",
    val working: Boolean = false
)

sealed class RetentionEvent {
    data class OnRequest(val item: FoundItem, val action: RetentionAction) : RetentionEvent()
    data class OnNoteChanged(val value: String) : RetentionEvent()
    data object OnConfirm : RetentionEvent()
    data object OnDismiss : RetentionEvent()
    /** Corre el mismo chequeo que el Worker diario (avisa al staff), sin esperar un día. */
    data object OnCheckNow : RetentionEvent()
    data object OnBack : RetentionEvent()
}

sealed class RetentionEffect {
    data object NavigateBack : RetentionEffect()
}

/** D04 · Pantalla de Retención del staff: vencidos (Donar / Descartar) y por vencer. */
class RetentionViewModel(
    private val observeRetention: ObserveRetentionUseCase,
    private val resolveExpired: ResolveExpiredItemUseCase,
    private val checkRetention: CheckRetentionUseCase,
    private val clock: Clock,
    /** F11: avisos unificados (snackbar). */
    private val messages: UserMessages
) : ViewModel() {

    private val _state = MutableStateFlow(RetentionState(now = clock.now()))
    val state: StateFlow<RetentionState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<RetentionEffect>()
    val effects: SharedFlow<RetentionEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            observeRetention()
                .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar la retención") } }
                .collect { overview -> _state.update { it.copy(loading = false, error = null, overview = overview, now = clock.now()) } }
        }
    }

    fun handleEvent(event: RetentionEvent) {
        when (event) {
            is RetentionEvent.OnRequest -> _state.update { it.copy(pending = PendingResolution(event.item, event.action), note = "") }
            is RetentionEvent.OnNoteChanged -> _state.update { it.copy(note = event.value.take(NOTE_MAX)) }
            RetentionEvent.OnDismiss -> _state.update { it.copy(pending = null, note = "") }
            RetentionEvent.OnConfirm -> confirm()
            RetentionEvent.OnCheckNow -> checkNow()
            RetentionEvent.OnBack -> viewModelScope.launch { _effects.emit(RetentionEffect.NavigateBack) }
        }
    }

    private fun confirm() {
        val pending = _state.value.pending ?: return
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val result = resolveExpired(pending.item.id.value, pending.action, _state.value.note)
            _state.update { it.copy(working = false, pending = null, note = "") }
            result.fold(
                onSuccess = { item -> messages.success("\"${item.title}\" quedó como ${pending.action.pastLabel}.") },
                onFailure = { e -> messages.error(e.message ?: "No se pudo completar") }
            )
        }
    }

    private fun checkNow() {
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            runCatching { checkRetention() }.fold(
                onSuccess = { check ->
                    if (check.notifiedStaff == 0) messages.show("No hay objetos vencidos ni por vencer.")
                    else {
                        val who = if (check.notifiedStaff == 1) "1 persona" else "${check.notifiedStaff} personas"
                        val expired = if (check.expired == 1) "1 vencido" else "${check.expired} vencidos"
                        messages.success("Se avisó a $who del staff: $expired y ${check.expiring} por vencer.")
                    }
                },
                onFailure = { e -> messages.error(e.message ?: "No se pudo revisar") }
            )
            _state.update { it.copy(working = false) }
        }
    }

    private companion object {
        const val NOTE_MAX = 120
    }
}
