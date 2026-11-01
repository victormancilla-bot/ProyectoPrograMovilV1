package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.domain.model.UserModel

/**
 * Un `object` (singleton de Kotlin) para que toda la app comparta la misma sesión.
 * Vive en memoria: S01 la hará persistente.
 */
object SessionStore {
    private var currentUser: UserModel? = null

    fun get(): UserModel? = currentUser
    fun set(user: UserModel) { currentUser = user }
    fun clear() { currentUser = null }
}
