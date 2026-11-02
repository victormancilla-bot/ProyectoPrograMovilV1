package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.AnswerControlQuestionsUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ClaimMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMatchDetailUseCase
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

/** Detalle de match (M05). Se actualiza solo desde Room (p. ej. si el staff confirma la coincidencia). */
class MatchDetailViewModel(
    private val matchId: ItemId,
    private val observeDetail: ObserveMatchDetailUseCase,
    private val claimMatch: ClaimMatchUseCase,
    private val rejectMatch: RejectMatchUseCase,
    private val answerQuestions: AnswerControlQuestionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MatchDetailEffect>()
    val effects: SharedFlow<MatchDetailEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun handleEvent(event: MatchDetailEvent) {
        when (event) {
            MatchDetailEvent.OnBack -> emit(MatchDetailEffect.NavigateBack)
            MatchDetailEvent.OnRetry -> load()
            MatchDetailEvent.OnClaim -> claim()
            MatchDetailEvent.OnReject -> if (state.value.canAct) {
                _state.value = state.value.copy(confirmingReject = true, actionError = null)
            }
            MatchDetailEvent.OnDismissReject -> _state.value = state.value.copy(confirmingReject = false)
            MatchDetailEvent.OnConfirmReject -> confirmReject()
            is MatchDetailEvent.OnAnswerChanged -> state.value.answers.let { answers ->
                if (event.index in answers.indices) {
                    _state.value = state.value.copy(
                        answers = answers.toMutableList().also { it[event.index] = event.text },
                        answersFeedback = null
                    )
                }
            }
            MatchDetailEvent.OnSubmitAnswers -> submitAnswers()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            observeDetail(matchId)
                .catch { _state.value = state.value.copy(loading = false, error = LOAD_ERROR) }
                .collect { detail ->
                    val current = state.value
                    // Un campo por pregunta; se conserva lo escrito si las preguntas no cambiaron.
                    val questions = detail?.existingClaim?.questions.orEmpty()
                    val answers = current.answers.takeIf { it.size == questions.size } ?: List(questions.size) { "" }
                    _state.value = current.copy(loading = false, error = null, detail = detail, answers = answers)
                }
        }
    }

    private fun claim() {
        val current = state.value
        if (!current.canAct || current.working) return
        _state.value = current.copy(working = true, actionError = null)
        viewModelScope.launch {
            claimMatch(matchId)
                .onSuccess { number -> _state.value = state.value.copy(working = false, claimNumber = number) }
                .onFailure { error ->
                    // Los errores de V02 ("Ya reclamaste este objeto...") se muestran tal cual; el resto, genérico.
                    val message = if (error is ClaimRequestException) error.message else CLAIM_ERROR
                    _state.value = state.value.copy(working = false, actionError = message)
                }
        }
    }

    private fun submitAnswers() {
        val current = state.value
        val claim = current.detail?.existingClaim ?: return
        if (!current.canSubmitAnswers) return
        _state.value = current.copy(working = true, answersFeedback = null)
        viewModelScope.launch {
            answerQuestions(claim.id, current.answers)
                .onSuccess { updated ->
                    // Si pasó a verificación o se bloqueó, la pantalla cambia sola con lo que llega de Room.
                    val feedback = if (updated.status == ClaimStatus.PENDIENTE) wrongAnswers(updated.attemptsLeft) else null
                    _state.value = state.value.copy(working = false, answersFeedback = feedback)
                }
                .onFailure { error ->
                    val message = if (error is ClaimRequestException) error.message else ANSWERS_ERROR
                    _state.value = state.value.copy(working = false, answersFeedback = message)
                }
        }
    }

    private fun confirmReject() {
        if (state.value.working) return
        _state.value = state.value.copy(confirmingReject = false, working = true)
        viewModelScope.launch {
            rejectMatch(matchId)
                .onSuccess {
                    _state.value = state.value.copy(working = false)
                    _effects.emit(MatchDetailEffect.NavigateBack)
                }
                .onFailure { _state.value = state.value.copy(working = false, actionError = REJECT_ERROR) }
        }
    }

    private fun emit(effect: MatchDetailEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val LOAD_ERROR = "No pudimos cargar esta coincidencia."
        const val CLAIM_ERROR = "No se pudo enviar el reclamo, intenta de nuevo."
        const val REJECT_ERROR = "No se pudo marcar como \"No es mío\", intenta de nuevo."
        const val ANSWERS_ERROR = "No se pudieron enviar tus respuestas, intenta de nuevo."

        /** No se dice cuál respuesta falló para no dar pistas a quien no es el dueño. */
        fun wrongAnswers(attemptsLeft: Int) =
            "Tus respuestas no coinciden con lo registrado. " +
                if (attemptsLeft == 1) "Te queda 1 intento." else "Te quedan $attemptsLeft intentos."
    }
}
