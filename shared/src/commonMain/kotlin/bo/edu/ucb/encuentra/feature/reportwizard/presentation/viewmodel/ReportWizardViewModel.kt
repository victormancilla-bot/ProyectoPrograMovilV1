package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Stub de F01: el dueño agrega Event/Effect y los use cases (ver LoginViewModel como ejemplo de MVI).
class ReportWizardViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReportWizardState())
    val state: StateFlow<ReportWizardState> = _state.asStateFlow()
}
