package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel compartido por los 4 pasos del wizard (L02-L04). Cada cambio revalida el paso actual. */
class ReportWizardViewModel(
    private val validateStep: ValidateReportStepUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(stateFor(step = 1, draft = ReportDraft()))
    val state: StateFlow<ReportDraftState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReportWizardEffect>()
    val effects: SharedFlow<ReportWizardEffect> = _effects.asSharedFlow()

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
            ReportWizardEvent.OnNext -> goToStep(state.value.step + 1)
            ReportWizardEvent.OnBack -> if (state.value.step == 1) {
                viewModelScope.launch { _effects.emit(ReportWizardEffect.Cancel) }
            } else {
                goToStep(state.value.step - 1)
            }
        }
    }

    private fun updateDraft(draft: ReportDraft) {
        _state.value = stateFor(state.value.step, draft).copy(showAllColors = state.value.showAllColors)
    }

    private fun goToStep(step: Int) {
        val current = state.value
        // Solo se avanza si el paso actual es válido; retroceder siempre se puede.
        if (step > current.step && !current.canContinue) return
        if (step !in 1..current.totalSteps) return
        _state.value = stateFor(step, current.draft).copy(showAllColors = current.showAllColors)
    }

    private fun stateFor(step: Int, draft: ReportDraft) = ReportDraftState(
        step = step,
        draft = draft,
        errors = validateStep(step, draft)
    )
}
