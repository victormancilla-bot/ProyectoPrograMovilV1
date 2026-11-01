package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
