package bo.edu.ucb.encuentra.feature.login.presentation.viewmodel

data class LoginState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
