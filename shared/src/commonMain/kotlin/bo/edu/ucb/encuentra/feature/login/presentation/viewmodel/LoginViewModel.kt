package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.Password
import bo.edu.ucb.encuentra.core.presentation.GoogleSignInResult
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.usecase.SignInWithGoogleUseCase
import bo.edu.ucb.encuentra.feature.login.domain.usecase.ValidateInstitutionalEmailUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import kotlinx.coroutines.launch

class LoginViewModel(
    private val validateEmail: ValidateInstitutionalEmailUseCase,
    private val authenticate: AuthenticateUseCase,
    /** F08: el correo de soporte lo define la oficina en Remote Config (`support_contact`). */
    private val getAppConfig: GetAppConfigUseCase,
    /** S02: cuenta Google de la UCB (Firebase Auth). */
    private val signInWithGoogle: SignInWithGoogleUseCase
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
            LoginEvent.OnContactSupport -> viewModelScope.launch {
                _effects.emit(LoginEffect.OpenSupport(supportUri(getAppConfig().supportContact)))
            }
            LoginEvent.OnGoogleStarted -> _state.update { it.copy(googleLoading = true, googleError = null) }
            is LoginEvent.OnGoogleResult -> onGoogleResult(event.result)
        }
    }

    private fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            GoogleSignInResult.Cancelled -> _state.update { it.copy(googleLoading = false) }
            is GoogleSignInResult.Failed -> _state.update { it.copy(googleLoading = false, googleError = result.message) }
            is GoogleSignInResult.Token -> viewModelScope.launch {
                _state.update { it.copy(googleLoading = true, googleError = null) }
                signInWithGoogle(result.idToken).fold(
                    onSuccess = {
                        _state.update { it.copy(googleLoading = false) }
                        _effects.emit(LoginEffect.NavigateToHome)
                    },
                    onFailure = { error -> _state.update { it.copy(googleLoading = false, googleError = error.message) } }
                )
            }
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

        /** "Contacta a Sistemas UCB": correo de `support_contact` (F08) con el asunto ya escrito. */
        fun supportUri(contact: String) = "mailto:$contact?subject=Problemas%20para%20ingresar%20a%20UCB%20Encuentra"
    }
}
