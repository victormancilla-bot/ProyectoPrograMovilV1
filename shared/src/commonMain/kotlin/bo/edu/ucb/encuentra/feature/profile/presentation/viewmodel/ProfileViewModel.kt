package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.presentation.UserMessages
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveDevicePreferencesUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveUserPreferencesUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SaveContactPhoneUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetLanguageUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetNotificationPreferenceUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetThemeUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S07 · Perfil y Ajustes: datos del usuario, preferencias (tema, idioma, avisos), WhatsApp y cerrar sesión. */
class ProfileViewModel(
    private val logoutUseCase: LogoutUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    observeDevice: ObserveDevicePreferencesUseCase,
    observeUserPrefs: ObserveUserPreferencesUseCase,
    private val setTheme: SetThemeUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val setNotification: SetNotificationPreferenceUseCase,
    private val saveContactPhone: SaveContactPhoneUseCase,
    /** F11: avisos unificados (snackbar). */
    private val messages: UserMessages
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState(user = getCurrentUser()))
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch { observeDevice().collect { device -> _state.update { it.copy(device = device) } } }
        viewModelScope.launch {
            observeUserPrefs().collect { prefs ->
                // El campo se llena con lo guardado, salvo que el usuario esté escribiendo otro número.
                _state.update { it.copy(prefs = prefs, phone = if (it.phoneChanged && it.phone.isNotEmpty()) it.phone else prefs.contactPhone) }
            }
        }
    }

    fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnThemeSelected -> viewModelScope.launch { setTheme(event.theme) }
            is ProfileEvent.OnLanguageSelected -> viewModelScope.launch { setLanguage(event.language) }
            is ProfileEvent.OnNotificationToggled -> viewModelScope.launch { setNotification(event.type, event.enabled) }
            is ProfileEvent.OnPhoneChanged -> _state.update { it.copy(phone = event.value, phoneError = null) }
            ProfileEvent.OnSavePhone -> savePhone()
            ProfileEvent.OnRequestLogout -> _state.update { it.copy(confirmLogout = true) }
            ProfileEvent.OnDismissLogout -> _state.update { it.copy(confirmLogout = false) }
            ProfileEvent.OnLogout -> logout()
        }
    }

    private fun savePhone() {
        viewModelScope.launch {
            saveContactPhone(_state.value.phone)
                .onSuccess { saved ->
                    _state.update { it.copy(phone = saved, phoneError = null) }
                    messages.success(if (saved.isEmpty()) "Se quitó tu WhatsApp de contacto." else "WhatsApp guardado: se usará en tus reportes.")
                }
                .onFailure { e -> _state.update { it.copy(phoneError = e.message) } }
        }
    }

    private fun logout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true, confirmLogout = false) }
        viewModelScope.launch {
            logoutUseCase()
            _effects.emit(ProfileEffect.NavigateToLogin)
        }
    }
}
