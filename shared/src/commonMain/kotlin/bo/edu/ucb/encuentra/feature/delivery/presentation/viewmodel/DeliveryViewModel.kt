package bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ConfirmDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ScheduleDeliveryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Confirmación de Entrega (V04). Programar es opcional (puede ser una entrega sin cita); confirmar exige el
 * checklist físico. El reclamo se lee en vivo de Room: tras confirmar, la pantalla muestra "Entregado".
 */
class DeliveryViewModel(
    private val claimId: ItemId,
    private val observeReview: ObserveClaimReviewUseCase,
    private val schedule: ScheduleDeliveryUseCase,
    private val confirmDelivery: ConfirmDeliveryUseCase,
    private val timeZone: TimeZone
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryState())
    val state: StateFlow<DeliveryState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DeliveryEffect>()
    val effects: SharedFlow<DeliveryEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: DeliveryEvent) {
        val current = state.value
        when (event) {
            DeliveryEvent.OnBack -> viewModelScope.launch { _effects.emit(DeliveryEffect.NavigateBack) }
            DeliveryEvent.OnRetry -> load()
            is DeliveryEvent.OnDatePicked -> _state.value = current.copy(date = event.date, info = null, actionError = null)
            is DeliveryEvent.OnTimePicked -> _state.value = current.copy(time = event.time, info = null, actionError = null)
            DeliveryEvent.OnSchedule -> scheduleDelivery()
            is DeliveryEvent.OnIdentityVerified ->
                _state.value = current.copy(checks = current.checks.copy(identityVerified = event.checked), actionError = null)
            is DeliveryEvent.OnItemReceived ->
                _state.value = current.copy(checks = current.checks.copy(itemReceived = event.checked), actionError = null)
            DeliveryEvent.OnConfirm -> confirm()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeReview(claimId)
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { review ->
                    val current = state.value
                    // Se prellena con lo ya programado, sin pisar lo que el staff esté eligiendo.
                    val scheduled = review?.claim?.scheduledDeliveryAt?.toLocalDateTime(timeZone)
                    _state.value = current.copy(
                        loading = false,
                        review = review,
                        date = current.date ?: scheduled?.date,
                        time = current.time ?: scheduled?.time
                    )
                }
        }
    }

    private fun scheduleDelivery() {
        val current = state.value
        val date = current.date ?: return
        val time = current.time ?: return
        if (!current.canSchedule) return
        _state.value = current.copy(working = true, actionError = null, info = null)
        viewModelScope.launch {
            schedule(claimId, LocalDateTime(date, time).toInstant(timeZone))
                .onSuccess { _state.value = state.value.copy(working = false, info = SCHEDULED) }
                .onFailure { _state.value = state.value.copy(working = false, actionError = it.userMessage()) }
        }
    }

    private fun confirm() {
        val current = state.value
        if (!current.canConfirm) return
        _state.value = current.copy(working = true, actionError = null)
        viewModelScope.launch {
            confirmDelivery(claimId, current.checks)
                // La pantalla cambia sola a "Entregado" con lo que llega de Room.
                .onSuccess { _state.value = state.value.copy(working = false, info = null) }
                .onFailure { _state.value = state.value.copy(working = false, actionError = it.userMessage()) }
        }
    }

    private fun Throwable.userMessage(): String = if (this is ClaimRequestException) message.orEmpty() else ACTION_ERROR

    companion object {
        const val LOAD_ERROR = "No pudimos cargar el reclamo."
        const val ACTION_ERROR = "No se pudo guardar, intenta de nuevo."
        const val SCHEDULED = "Entrega programada. Le avisamos al estudiante."
    }
}
