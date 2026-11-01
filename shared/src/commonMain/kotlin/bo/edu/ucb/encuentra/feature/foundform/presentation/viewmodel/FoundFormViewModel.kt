package bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Stub de F01: el dueño agrega Event/Effect y los use cases (ver LoginViewModel como ejemplo de MVI).
class FoundFormViewModel : ViewModel() {

    private val _state = MutableStateFlow(FoundFormState())
    val state: StateFlow<FoundFormState> = _state.asStateFlow()
}
