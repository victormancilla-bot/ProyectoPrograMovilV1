package bo.edu.ucb.encuentra.feature.login.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.RemoteUserStore
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email

/**
 * S02 · Ingreso con la cuenta Google de la UCB (Firebase Auth):
 * 1. Firebase valida el token de Google (no hay cuentas anónimas);
 * 2. solo @ucb.edu.bo: cualquier otra cuenta se cierra al momento (también lo exigen las reglas de Firestore);
 * 3. `users/{uid}`: se crea con rol ESTUDIANTE al primer ingreso o se lee el rol que le asignó un admin (S03);
 * 4. se guarda en Room (el resto de la app lee los usuarios de ahí) y queda la sesión persistente (S01).
 */
class SignInWithGoogleUseCase(
    private val remoteAuth: RemoteAuth,
    private val remoteUsers: RemoteUserStore,
    private val users: UserRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(idToken: String): Result<UserModel> = runCatching {
        val account = try {
            remoteAuth.signInWithGoogle(idToken)
        } catch (e: Exception) {
            throw IllegalStateException(GOOGLE_FAILED, e)
        }
        if (!Email(account.email).isInstitutional()) {
            remoteAuth.signOut()
            throw IllegalArgumentException(NOT_INSTITUTIONAL)
        }
        val user = remoteUsers.getOrCreate(account)
        users.save(user)
        session.setCurrentUser(user)
        user
    }

    companion object {
        const val NOT_INSTITUTIONAL = "Usa tu cuenta institucional @ucb.edu.bo"
        const val GOOGLE_FAILED = "No se pudo ingresar con Google. Revisa tu conexión e inténtalo de nuevo."
    }
}
