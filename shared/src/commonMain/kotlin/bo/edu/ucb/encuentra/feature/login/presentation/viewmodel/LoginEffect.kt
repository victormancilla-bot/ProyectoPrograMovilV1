package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()

    /** Abre el contacto de Sistemas UCB (correo o página) con el manejador de enlaces del sistema. */
    data class OpenSupport(val uri: String) : LoginEffect()
}
