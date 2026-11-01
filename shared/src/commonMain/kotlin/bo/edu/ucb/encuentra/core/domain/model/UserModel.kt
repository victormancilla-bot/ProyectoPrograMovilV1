package bo.edu.ucb.encuentra.core.domain.model

/**
 * Usuario autenticado. Es "core" (no pertenece a un solo feature)
 * porque Login, la navegación (guard) y Perfil lo necesitan.
 */
data class UserModel(
    val id: String,
    val fullName: String,
    val email: String,
    val role: Role
)
