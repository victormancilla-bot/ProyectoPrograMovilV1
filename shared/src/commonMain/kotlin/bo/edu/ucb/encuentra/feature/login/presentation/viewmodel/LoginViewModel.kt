package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.Password
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.usecase.ValidateInstitutionalEmailUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val validateEmail: ValidateInstitutionalEmailUseCase,
    private val authenticate: AuthenticateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun handleEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnEmailChanged -> _state.update { it.copy(email = event.email, emailError = null) }
            is LoginEvent.OnPasswordChanged -> _state.update { it.copy(password = event.password, passwordError = null) }
            LoginEvent.OnContinue -> when (state.value.step) {
                LoginStep.EMAIL -> continueWithEmail()
                LoginStep.PASSWORD -> signIn()
            }
            LoginEvent.OnChangeEmail -> _state.update {
                it.copy(step = LoginStep.EMAIL, password = "", passwordError = null)
            }
            LoginEvent.OnContactSupport -> viewModelScope.launch { _effects.emit(LoginEffect.OpenSupport(SUPPORT_URI)) }
        }
    }

    private fun continueWithEmail() {
        validateEmail(state.value.email).fold(
            onSuccess = { email -> _state.update { it.copy(email = email.value, step = LoginStep.PASSWORD) } },
            onFailure = { error -> _state.update { it.copy(emailError = error.message) } }
        )
    }

    private fun signIn() {
        val current = state.value
        if (current.loading) return
        if (current.password.isBlank()) {
            _state.update { it.copy(passwordError = EMPTY_PASSWORD) }
            return
        }
        _state.update { it.copy(loading = true, passwordError = null) }
        viewModelScope.launch {
            authenticate(Email(current.email), Password(current.password)).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    _effects.emit(LoginEffect.NavigateToHome)
                },
                onFailure = { error -> _state.update { it.copy(loading = false, passwordError = error.message) } }
            )
        }
    }

    companion object {
        const val EMPTY_PASSWORD = "Ingresa tu contraseña"

        // ⚠️ Contacto de prueba: confirmar el correo/página real de Sistemas UCB (o leerlo de Remote Config, F08).
        const val SUPPORT_URI = "mailto:sistemas@ucb.edu.bo?subject=Problemas%20para%20ingresar%20a%20UCB%20Encuentra"
    }
}
