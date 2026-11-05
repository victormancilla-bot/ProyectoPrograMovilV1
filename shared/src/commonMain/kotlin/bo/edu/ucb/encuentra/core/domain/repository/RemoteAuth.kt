package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.UserModel

/** Cuenta autenticada en Firebase (S02). */
data class RemoteAccount(val uid: String, val email: String, val displayName: String?)

/** Firebase Auth (S02): iniciar sesión con el token de Google, saber si sigue válida y cerrarla. */
interface RemoteAuth {
    suspend fun signInWithGoogle(idToken: String): RemoteAccount

    /** uid de la sesión de Firebase en este teléfono; null si no hay o expiró. */
    fun currentUid(): String?

    suspend fun signOut()
}

/**
 * `users/{uid}` en Firestore (S02/S03): al primer ingreso se crea con rol ESTUDIANTE (las reglas no dejan crearla
 * con otro rol); si ya existe, se respeta el rol que le dio un admin.
 */
interface RemoteUserStore {
    suspend fun getOrCreate(account: RemoteAccount): UserModel
}

/** Sin Firebase (iOS/tests): no hay sesión remota. */
object NoRemoteAuth : RemoteAuth {
    override suspend fun signInWithGoogle(idToken: String): RemoteAccount = error("Inicio de sesión con Google no disponible")
    override fun currentUid(): String? = null
    override suspend fun signOut() = Unit
}
