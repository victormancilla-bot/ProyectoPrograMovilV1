package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Base de F05: solo el paso actual. L02-L04 agregan el borrador (categoría, atributos, lugar, foto…).
class ReportWizardViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReportWizardState())
    val state: StateFlow<ReportWizardState> = _state.asStateFlow()

    fun handleEvent(event: ReportWizardEvent) {
        when (event) {
            ReportWizardEvent.OnNext -> _state.update { it.copy(step = (it.step + 1).coerceAtMost(it.totalSteps)) }
            ReportWizardEvent.OnPrevious -> _state.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
        }
    }
}
