package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.GetHowItWorksUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.HowItWorksStep
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HowItWorksState(val steps: List<HowItWorksStep> = emptyList())

sealed class HowItWorksEvent {
    data object OnReport : HowItWorksEvent()
    data object OnBack : HowItWorksEvent()
}

sealed class HowItWorksEffect {
    data object NavigateToReport : HowItWorksEffect()
    data object NavigateBack : HowItWorksEffect()
}

/** L07 · "Cómo funciona" en 4 pasos. */
class HowItWorksViewModel(getHowItWorks: GetHowItWorksUseCase) : ViewModel() {

    private val _state = MutableStateFlow(HowItWorksState(steps = getHowItWorks()))
    val state: StateFlow<HowItWorksState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HowItWorksEffect>()
    val effects: SharedFlow<HowItWorksEffect> = _effects.asSharedFlow()

    fun handleEvent(event: HowItWorksEvent) {
        viewModelScope.launch {
            _effects.emit(
                when (event) {
                    HowItWorksEvent.OnReport -> HowItWorksEffect.NavigateToReport
                    HowItWorksEvent.OnBack -> HowItWorksEffect.NavigateBack
                }
            )
        }
    }
}
