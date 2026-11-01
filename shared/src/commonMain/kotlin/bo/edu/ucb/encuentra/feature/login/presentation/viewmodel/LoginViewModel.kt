package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.vo.Email
import bo.edu.ucb.encuentra.feature.login.domain.vo.Password

class LoginViewModel(
    private val authenticateUseCase: AuthenticateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun handleEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is LoginEvent.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            LoginEvent.OnSubmit -> {
                submit()
            }
        }
    }

    private fun submit() {
        val currentState = state.value
        val email = currentState.email
        val password = currentState.password

        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch {
                _effects.emit(LoginEffect.ShowToast("Email y password son requeridos"))
            }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = authenticateUseCase(Email(email), Password(password))
            result.fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    _effects.emit(LoginEffect.NavigateToHome)
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.message) }
                    _effects.emit(LoginEffect.ShowToast(error.message ?: "Error"))
                }
            )
        }
    }
}
