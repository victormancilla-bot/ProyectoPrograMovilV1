package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

import bo.edu.ucb.encuentra.core.presentation.GoogleSignInResult

sealed class LoginEvent {
    data class OnEmailChanged(val email: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()

    /** Botón "Continuar": en el paso EMAIL valida el correo; en PASSWORD inicia sesión. */
    data object OnContinue : LoginEvent()

    /** "Cambiar correo": vuelve al paso EMAIL. */
    data object OnChangeEmail : LoginEvent()

    /** Enlace "Contacta a Sistemas UCB". */
    data object OnContactSupport : LoginEvent()

    /** S02: tocó "Continuar con Google" (se abre el selector de cuentas). */
    data object OnGoogleStarted : LoginEvent()

    /** S02: lo que devolvió el selector de cuentas de Google. */
    data class OnGoogleResult(val result: GoogleSignInResult) : LoginEvent()
}
