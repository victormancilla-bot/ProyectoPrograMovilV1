package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

/** El login tiene dos pasos en la misma tarjeta: primero el correo (como el Figma) y luego la contraseña. */
enum class LoginStep { EMAIL, PASSWORD }

data class LoginState(
    val step: LoginStep = LoginStep.EMAIL,
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loading: Boolean = false,
    /** S02: esperando a Google/Firebase. */
    val googleLoading: Boolean = false,
    val googleError: String? = null
)
