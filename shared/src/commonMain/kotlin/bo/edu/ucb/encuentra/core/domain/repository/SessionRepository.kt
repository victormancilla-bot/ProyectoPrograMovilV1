package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.UserModel

/**
 * Sabe quién es el usuario que inició sesión. La sesión se guarda en el dispositivo (S01):
 * al abrir la app [restore] la recupera y [getCurrentUser] la devuelve sin esperar.
 */
interface SessionRepository {
    fun getCurrentUser(): UserModel?
    suspend fun setCurrentUser(user: UserModel)
    suspend fun clearSession()
    suspend fun restore()
}
