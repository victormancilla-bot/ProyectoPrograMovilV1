package bo.edu.ucb.encuentra.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.home.domain.usecase.ObserveHomeActivityUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    getCurrentUser: GetCurrentUserUseCase,
    observeHomeActivity: ObserveHomeActivityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(headerFor(getCurrentUser()))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HomeEffect>()
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    init {
        // Room emite de nuevo cada vez que cambian los reportes o las notificaciones: el Home se actualiza solo.
        viewModelScope.launch {
            observeHomeActivity().collect { activity ->
                _state.update {
                    it.copy(activeReports = activity.activeReports, newMatches = activity.newMatches, loading = false)
                }
            }
        }
    }

    fun handleEvent(event: HomeEvent) {
        val effect = when (event) {
            HomeEvent.OnReportObject -> HomeEffect.NavigateToReportWizard
            HomeEvent.OnActiveReportsClick, HomeEvent.OnMyReportsClick -> HomeEffect.NavigateToMyReports
            HomeEvent.OnNewMatchesClick -> HomeEffect.NavigateToMatches
            HomeEvent.OnFoundItemsClick -> HomeEffect.NavigateToFoundCatalog
            HomeEvent.OnHowItWorksClick -> HomeEffect.NavigateToHowItWorks
        }
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun headerFor(user: UserModel?): HomeState {
        if (user == null) return HomeState()
        val firstName = user.fullName.trim().substringBefore(" ")
        return HomeState(
            greeting = "¡Hola, $firstName!",
            subtitle = listOfNotNull(user.faculty, user.campus.label).joinToString(" • ")
        )
    }
}
