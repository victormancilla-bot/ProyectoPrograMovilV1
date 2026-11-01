package bo.edu.ucb.encuentra.core.domain.model

/** Roles de la app (S03). STAFF y ADMIN comparten la navegación de staff. */
enum class Role {
    ESTUDIANTE,
    STAFF,
    ADMIN;

    val isStaff: Boolean get() = this == STAFF || this == ADMIN
}
