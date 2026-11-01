package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Stub de F01: el dueño agrega Event/Effect y los use cases (ver LoginViewModel como ejemplo de MVI).
class HowItWorksViewModel : ViewModel() {

    private val _state = MutableStateFlow(HowItWorksState())
    val state: StateFlow<HowItWorksState> = _state.asStateFlow()
}
