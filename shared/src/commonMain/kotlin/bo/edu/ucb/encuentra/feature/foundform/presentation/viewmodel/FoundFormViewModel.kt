package bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.RegisterFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Formulario "Registrar Objeto" (H03). Fecha y hora del hallazgo arrancan en "ahora". */
class FoundFormViewModel(
    private val validate: ValidateFoundItemUseCase,
    private val register: RegisterFoundItemUseCase,
    clock: Clock,
    timeZone: TimeZone
) : ViewModel() {

    private val _state = MutableStateFlow(
        clock.now().toLocalDateTime(timeZone).let { now ->
            val draft = FoundItemDraft(foundDate = now.date, foundTime = LocalTime(now.hour, now.minute))
            FoundFormState(draft = draft, errors = validate(draft))
        }
    )
    val state: StateFlow<FoundFormState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<FoundFormEffect>()
    val effects: SharedFlow<FoundFormEffect> = _effects.asSharedFlow()

    fun handleEvent(event: FoundFormEvent) {
        val draft = state.value.draft
        val questions = draft.controlQuestions
        // Una respuesta esperada por pregunta, en la misma posición (V01).
        val answers = draft.expectedAnswers.let { it + List((questions.size - it.size).coerceAtLeast(0)) { "" } }
        when (event) {
            is FoundFormEvent.OnCategorySelected -> updateDraft(
                // Al cambiar de categoría, la subcategoría anterior ya no corresponde.
                draft.copy(
                    category = event.category,
                    subcategory = draft.subcategory?.takeIf { it.category == event.category }
                )
            )
            is FoundFormEvent.OnSubcategorySelected -> updateDraft(draft.copy(subcategory = event.subcategory))
            is FoundFormEvent.OnColorSelected -> updateDraft(draft.copy(color = event.color))
            FoundFormEvent.OnShowAllColors -> _state.value = state.value.copy(showAllColors = true)
            is FoundFormEvent.OnTitleChanged -> updateDraft(draft.copy(title = event.title))
            is FoundFormEvent.OnBrandChanged -> updateDraft(draft.copy(brand = event.brand))
            is FoundFormEvent.OnFeaturesChanged -> updateDraft(draft.copy(features = event.features))
            is FoundFormEvent.OnFoundZoneSelected -> updateDraft(draft.copy(foundZone = event.zone))
            is FoundFormEvent.OnCustodySelected -> updateDraft(draft.copy(custodyPlace = event.place))
            is FoundFormEvent.OnCustodyDetailChanged -> updateDraft(draft.copy(custodyDetail = event.detail))
            is FoundFormEvent.OnFoundDateSelected -> updateDraft(draft.copy(foundDate = event.date))
            is FoundFormEvent.OnFoundTimeSelected -> updateDraft(draft.copy(foundTime = event.time))
            is FoundFormEvent.OnQuestionChanged -> if (event.index in questions.indices) {
                updateDraft(draft.copy(controlQuestions = questions.toMutableList().also { it[event.index] = event.text }))
            }
            is FoundFormEvent.OnExpectedAnswerChanged -> if (event.index in questions.indices) {
                updateDraft(draft.copy(expectedAnswers = answers.toMutableList().also { it[event.index] = event.text }))
            }
            FoundFormEvent.OnAddQuestion -> if (state.value.canAddQuestion) {
                updateDraft(draft.copy(controlQuestions = questions + "", expectedAnswers = answers.take(questions.size) + ""))
            }
            // Siempre queda al menos un campo de pregunta.
            is FoundFormEvent.OnRemoveQuestion -> if (questions.size > 1 && event.index in questions.indices) {
                updateDraft(
                    draft.copy(
                        controlQuestions = questions.filterIndexed { i, _ -> i != event.index },
                        expectedAnswers = answers.take(questions.size).filterIndexed { i, _ -> i != event.index }
                    )
                )
            }
            FoundFormEvent.OnSubmit -> submit()
            FoundFormEvent.OnDismissError -> _state.value = state.value.copy(error = null)
        }
    }

    private fun updateDraft(draft: FoundItemDraft) {
        _state.value = state.value.copy(draft = draft, errors = validate(draft))
    }

    private fun submit() {
        val current = state.value
        if (current.saving) return
        if (current.errors.isNotEmpty()) {
            _state.value = current.copy(showErrors = true)
            return
        }
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            register(current.draft)
                .onSuccess { item ->
                    _state.value = state.value.copy(saving = false)
                    _effects.emit(FoundFormEffect.Registered(item.title))
                }
                .onFailure { e ->
                    _state.value = state.value.copy(saving = false, showErrors = true, error = e.message ?: SAVE_ERROR)
                }
        }
    }

    companion object {
        const val SAVE_ERROR = "No se pudo registrar el objeto, intenta de nuevo"
    }
}
