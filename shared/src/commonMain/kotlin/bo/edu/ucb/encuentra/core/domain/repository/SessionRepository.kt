package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.UserModel

/**
 * Sabe quién es el usuario que inició sesión en este momento.
 * La usan Login (para guardarlo), la navegación (guard) y Perfil (logout).
 */
interface SessionRepository {
    fun getCurrentUser(): UserModel?
    fun setCurrentUser(user: UserModel)
    fun clearSession()
}
