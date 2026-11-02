package bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Detalle de reclamo para el staff (V01; V03 le pone el diseño final). La app sugiere "coincide / no coincide"
 * por respuesta, pero aprobar o rechazar es siempre una decisión humana, con confirmación.
 */
class ClaimDetailViewModel(
    private val claimId: ItemId,
    private val observeReview: ObserveClaimReviewUseCase,
    private val approve: ApproveClaimUseCase,
    private val reject: RejectClaimUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(ClaimDetailState())
    val state: StateFlow<ClaimDetailState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ClaimDetailEffect>()
    val effects: SharedFlow<ClaimDetailEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: ClaimDetailEvent) {
        when (event) {
            ClaimDetailEvent.OnBack -> viewModelScope.launch { _effects.emit(ClaimDetailEffect.NavigateBack) }
            ClaimDetailEvent.OnRetry -> load()
            is ClaimDetailEvent.OnDecide -> {
                val review = state.value.review ?: return
                val allowed = if (event.decision == ClaimDecision.APPROVE) review.canApprove else review.canReject
                if (allowed) {
                    _state.value = state.value.copy(confirming = event.decision, note = "", reasonMissing = false, actionError = null)
                }
            }
            is ClaimDetailEvent.OnNoteChanged -> _state.value = state.value.copy(note = event.note, reasonMissing = false)
            ClaimDetailEvent.OnDismiss -> _state.value = state.value.copy(confirming = null)
            ClaimDetailEvent.OnConfirm -> confirm()
            ClaimDetailEvent.OnOpenDelivery -> viewModelScope.launch {
                _effects.emit(ClaimDetailEffect.NavigateToDelivery(claimId.value))
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeReview(claimId)
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { review -> _state.value = state.value.copy(loading = false, review = review, now = clock.now()) }
        }
    }

    private fun confirm() {
        val current = state.value
        val decision = current.confirming ?: return
        if (current.working) return
        // El diálogo queda abierto hasta que haya un motivo (también lo exige el dominio).
        if (current.note.isBlank()) {
            _state.value = current.copy(reasonMissing = true)
            return
        }
        _state.value = current.copy(confirming = null, working = true)
        viewModelScope.launch {
            val note = current.note.trim()
            val result = when (decision) {
                ClaimDecision.APPROVE -> approve(claimId, note)
                ClaimDecision.REJECT -> reject(claimId, note)
            }
            // El detalle se actualiza solo desde Room con el nuevo estado.
            result.onSuccess { _state.value = state.value.copy(working = false) }
                .onFailure { error ->
                    val message = if (error is ClaimRequestException) error.message else DECISION_ERROR
                    _state.value = state.value.copy(working = false, actionError = message)
                }
        }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar el reclamo."
        const val DECISION_ERROR = "No se pudo guardar la decisión, intenta de nuevo."
    }
}
