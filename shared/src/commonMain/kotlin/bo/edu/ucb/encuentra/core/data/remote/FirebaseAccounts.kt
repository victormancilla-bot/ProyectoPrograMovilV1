package bo.edu.ucb.encuentra.core.data.remote

import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAccount
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.RemoteUserStore
import bo.edu.ucb.encuentra.core.domain.vo.Email
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

/**
 * S02 · Firebase Auth (GitLive, commonMain). Si la app no tiene `google-services.json` (otro integrante sin
 * configurar Firebase), Firebase no está inicializado: [currentUid] responde null y el ingreso con Google falla
 * con un mensaje en vez de cerrar la app.
 */
class FirebaseRemoteAuth : RemoteAuth {

    override suspend fun signInWithGoogle(idToken: String): RemoteAccount {
        val user = Firebase.auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null)).user
            ?: error("Firebase no devolvió el usuario")
        return RemoteAccount(user.uid, user.email.orEmpty(), user.displayName)
    }

    override fun currentUid(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

    override suspend fun signOut() {
        runCatching { Firebase.auth.signOut() }
    }
}

/** Documento `users/{uid}`: los mismos campos que valida `firebase/firestore.rules`. */
@Serializable
data class UserDocument(
    val email: String,
    val fullName: String,
    val role: String = Role.ESTUDIANTE.name,
    val campus: String = Campus.LA_PAZ.name,
    val faculty: String? = null
)

/** `users/{uid}` en Firestore: se crea con rol ESTUDIANTE al primer ingreso; si existe, se respeta su rol. */
class FirestoreUserStore : RemoteUserStore {

    override suspend fun getOrCreate(account: RemoteAccount): UserModel {
        val doc = Firebase.firestore.collection(USERS).document(account.uid)
        val snapshot = doc.get()
        val data = if (snapshot.exists) {
            // Versiones no inline: las reificadas de GitLive vienen compiladas para JVM 17 y el proyecto usa 11.
            snapshot.data(UserDocument.serializer())
        } else {
            UserDocument(email = account.email.lowercase(), fullName = account.displayName ?: account.email.substringBefore('@'))
                .also { doc.set(UserDocument.serializer(), it) }
        }
        return data.toModel(account.uid)
    }

    private companion object {
        const val USERS = "users"
    }
}

internal fun UserDocument.toModel(uid: String) = UserModel(
    id = uid,
    fullName = fullName,
    email = Email(email),
    role = Role.entries.firstOrNull { it.name == role } ?: Role.ESTUDIANTE,
    campus = Campus.entries.firstOrNull { it.name == campus } ?: Campus.LA_PAZ,
    faculty = faculty
)
