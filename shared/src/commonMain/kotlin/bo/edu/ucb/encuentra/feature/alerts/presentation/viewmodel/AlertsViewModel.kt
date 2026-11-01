package bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Stub de F01: el dueño agrega Event/Effect y los use cases (ver LoginViewModel como ejemplo de MVI).
class AlertsViewModel : ViewModel() {

    private val _state = MutableStateFlow(AlertsState())
    val state: StateFlow<AlertsState> = _state.asStateFlow()
}
