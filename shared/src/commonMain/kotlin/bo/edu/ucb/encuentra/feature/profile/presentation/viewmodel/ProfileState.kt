package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.DevicePreferences
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.model.UserPreferences

data class ProfileState(
    val user: UserModel? = null,
    val device: DevicePreferences = DevicePreferences(),
    val prefs: UserPreferences = UserPreferences(),
    /** Lo que se está escribiendo en el campo de WhatsApp (se guarda con el botón). */
    val phone: String = "",
    val phoneError: String? = null,
    val message: String? = null,
    val confirmLogout: Boolean = false,
    val loggingOut: Boolean = false
) {
    /** S03: solo el ADMIN ve "Roles y permisos". */
    val canManageRoles: Boolean get() = user?.role == Role.ADMIN

    /** Avisos que le llegan a cada rol: el estudiante, coincidencias y reclamos; el staff, retención. */
    val notificationTypes: List<NotificationType>
        get() = if (user?.role?.isStaff == true) listOf(NotificationType.RETENTION)
        else listOf(NotificationType.NEW_MATCH, NotificationType.CLAIM_UPDATE)

    val phoneChanged: Boolean get() = phone.trim() != prefs.contactPhone
}
