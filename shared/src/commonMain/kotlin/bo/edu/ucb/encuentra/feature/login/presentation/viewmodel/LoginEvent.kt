package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

sealed class LoginEvent {
    data class OnEmailChanged(val email: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()
    data object OnSubmit : LoginEvent()
}
