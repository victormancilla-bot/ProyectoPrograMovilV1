package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

sealed class LoginEvent {
    data class OnEmailChanged(val email: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()

    /** Botón "Continuar": en el paso EMAIL valida el correo; en PASSWORD inicia sesión. */
    data object OnContinue : LoginEvent()

    /** "Cambiar correo": vuelve al paso EMAIL. */
    data object OnChangeEmail : LoginEvent()

    /** Enlace "Contacta a Sistemas UCB". */
    data object OnContactSupport : LoginEvent()
}
