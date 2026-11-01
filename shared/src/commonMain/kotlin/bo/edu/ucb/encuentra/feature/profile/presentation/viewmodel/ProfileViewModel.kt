package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Stub de F01 + logout (F05). El dueño agrega el resto de Perfil y Ajustes (S07).
class ProfileViewModel(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    fun handleEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.OnLogout -> {
                logoutUseCase()
                viewModelScope.launch { _effects.emit(ProfileEffect.NavigateToLogin) }
            }
        }
    }
}
