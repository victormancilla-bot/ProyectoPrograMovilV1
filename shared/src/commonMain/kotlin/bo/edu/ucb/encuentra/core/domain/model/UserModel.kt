package bo.edu.ucb.encuentra.core.domain.model

import bo.edu.ucb.encuentra.core.domain.vo.Email

/**
 * Usuario autenticado. Es "core" (no pertenece a un solo feature)
 * porque Login, la navegación (guard), Perfil y los reportes lo necesitan.
 * [faculty] y [campus] arman el subtítulo del header: "Ingeniería • Sede La Paz".
 */
data class UserModel(
    val id: String,
    val fullName: String,
    val email: Email,
    val role: Role,
    val campus: Campus,
    val faculty: String? = null
)
