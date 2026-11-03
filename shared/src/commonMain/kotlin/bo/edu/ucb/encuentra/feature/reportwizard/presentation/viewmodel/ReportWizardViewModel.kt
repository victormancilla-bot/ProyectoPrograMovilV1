package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.usecase.GetContactPhoneUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.SubmitLostReportUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel compartido por los 4 pasos del wizard (L02-L04). Cada cambio revalida el paso actual. */
class ReportWizardViewModel(
    private val validateStep: ValidateReportStepUseCase,
    private val submitReport: SubmitLostReportUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    getContactPhone: GetContactPhoneUseCase
) : ViewModel() {

    // Fechas en "hoy" y contacto prellenado con el correo del perfil (L03).
    private val _state = MutableStateFlow(
        validateStep.today().let { today ->
            val draft = ReportDraft(
                lostFrom = today,
                lostTo = today,
                contactEmail = getCurrentUser()?.email?.value.orEmpty()
            )
            ReportDraftState(draft = draft, errors = validateStep(1, draft))
        }
    )
    val state: StateFlow<ReportDraftState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReportWizardEffect>()
    val effects: SharedFlow<ReportWizardEffect> = _effects.asSharedFlow()

    init {
        // S07: el WhatsApp guardado en el Perfil se prellena (si el usuario todavía no escribió otro).
        viewModelScope.launch {
            val phone = getContactPhone()
            if (phone.isNotEmpty()) {
                _state.update { state ->
                    if (state.draft.contactPhone.isNotEmpty()) state else state.copy(draft = state.draft.copy(contactPhone = phone))
                }
            }
        }
    }

    fun handleEvent(event: ReportWizardEvent) {
        val draft = state.value.draft
        when (event) {
            is ReportWizardEvent.OnCategorySelected -> updateDraft(
                // Al cambiar de categoría, la subcategoría anterior ya no corresponde.
                draft.copy(
                    category = event.category,
                    subcategory = draft.subcategory?.takeIf { it.category == event.category }
                )
            )
            is ReportWizardEvent.OnSubcategorySelected -> updateDraft(draft.copy(subcategory = event.subcategory))
            is ReportWizardEvent.OnTitleChanged -> updateDraft(draft.copy(title = event.title))
            is ReportWizardEvent.OnColorSelected -> updateDraft(draft.copy(color = event.color))
            ReportWizardEvent.OnShowAllColors -> _state.value = state.value.copy(showAllColors = true)
            is ReportWizardEvent.OnBrandChanged -> updateDraft(draft.copy(brand = event.brand))
            is ReportWizardEvent.OnFeaturesChanged -> updateDraft(draft.copy(features = event.features))
            is ReportWizardEvent.OnZoneSelected -> updateDraft(draft.copy(zone = event.zone))
            is ReportWizardEvent.OnLocationDetailChanged -> updateDraft(draft.copy(locationDetail = event.detail))
            // Si "Desde" pasa a "Hasta", "Hasta" lo sigue: el rango no queda invertido por elegir en otro orden.
            is ReportWizardEvent.OnLostFromSelected -> updateDraft(
                draft.copy(lostFrom = event.date, lostTo = draft.lostTo?.takeIf { it >= event.date } ?: event.date)
            )
            is ReportWizardEvent.OnLostToSelected -> updateDraft(draft.copy(lostTo = event.date))
            is ReportWizardEvent.OnTimeSlotSelected -> updateDraft(draft.copy(timeSlot = event.slot))
            is ReportWizardEvent.OnPhotoPicked -> updateDraft(draft.copy(photoUri = event.uri))
            ReportWizardEvent.OnPhotoRemoved -> updateDraft(draft.copy(photoUri = null))
            is ReportWizardEvent.OnPhoneChanged -> updateDraft(draft.copy(contactPhone = event.phone))

            is ReportWizardEvent.OnEditStep -> if (state.value.isConfirmation && event.step in 1 until ReportDraftState.TOTAL_STEPS) {
                showStep(event.step, editingFromSummary = true)
            }
            ReportWizardEvent.OnSubmit -> submit()
            ReportWizardEvent.OnDismissSubmitError -> _state.value = state.value.copy(submitError = null)
            ReportWizardEvent.OnGoToMyReports -> emit(ReportWizardEffect.GoToMyReports)
            ReportWizardEvent.OnFinish -> emit(ReportWizardEffect.Cancel)

            ReportWizardEvent.OnNext -> next()
            ReportWizardEvent.OnBack -> back()
        }
    }

    private fun next() {
        val current = state.value
        if (!current.canContinue || current.isConfirmation) return
        // Editando desde el resumen: si todo sigue válido, vuelve directo a la confirmación.
        val target = if (current.editingFromSummary && allStepsValid(current.draft)) ReportDraftState.TOTAL_STEPS else current.step + 1
        showStep(target, editingFromSummary = current.editingFromSummary && target != ReportDraftState.TOTAL_STEPS)
    }

    private fun back() {
        val current = state.value
        when {
            // Después de enviar, atrás sale del wizard (el reporte ya está guardado).
            current.submitted != null -> emit(ReportWizardEffect.Cancel)
            current.submitting -> Unit
            current.editingFromSummary && allStepsValid(current.draft) -> showStep(ReportDraftState.TOTAL_STEPS, editingFromSummary = false)
            current.step == 1 -> emit(ReportWizardEffect.Cancel)
            else -> showStep(current.step - 1, editingFromSummary = current.editingFromSummary)
        }
    }

    private fun submit() {
        val current = state.value
        if (!current.isConfirmation || current.submitting || current.submitted != null) return
        _state.value = current.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            submitReport(current.draft)
                .onSuccess { report -> _state.value = state.value.copy(submitting = false, submitted = report) }
                .onFailure { e ->
                    _state.value = state.value.copy(submitting = false, submitError = e.message ?: SUBMIT_ERROR)
                }
        }
    }

    private fun allStepsValid(draft: ReportDraft) =
        validateStep.upTo(ValidateReportStepUseCase.STEP_WHERE_WHEN, draft).isEmpty()

    private fun updateDraft(draft: ReportDraft) {
        _state.value = state.value.copy(draft = draft, errors = validateStep(state.value.step, draft))
    }

    private fun showStep(step: Int, editingFromSummary: Boolean) {
        val current = state.value
        _state.value = current.copy(
            step = step,
            errors = validateStep(step, current.draft),
            editingFromSummary = editingFromSummary
        )
    }

    private fun emit(effect: ReportWizardEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        const val SUBMIT_ERROR = "No se pudo enviar el reporte, intenta de nuevo"
    }
}
