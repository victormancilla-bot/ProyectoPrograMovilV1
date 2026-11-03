package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

data class ProfileState(
    val loading: Boolean = false,
    val error: String? = null,
    /** S03: solo el ADMIN ve "Roles y permisos". */
    val canManageRoles: Boolean = false
)
