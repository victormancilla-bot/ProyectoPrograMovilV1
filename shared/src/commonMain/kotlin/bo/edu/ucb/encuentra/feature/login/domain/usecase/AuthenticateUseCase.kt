package bo.edu.ucb.encuentra.feature.login.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.feature.login.domain.vo.Email
import bo.edu.ucb.encuentra.feature.login.domain.vo.Password
import kotlinx.coroutines.delay

/**
 * Login de prueba hasta S01/S02 (Firebase Auth). Si las credenciales son correctas guarda la sesión.
 */
class AuthenticateUseCase(
    private val session: SessionRepository
) {
    suspend operator fun invoke(email: Email, password: Password): Result<UserModel> {
        delay(1000)
        val user = DEMO_USERS[email.value]
        return if (user != null && password.value == DEMO_PASSWORD) {
            session.setCurrentUser(user)
            Result.success(user)
        } else {
            Result.failure(Exception("Credenciales incorrectas"))
        }
    }

    companion object {
        const val DEMO_PASSWORD = "password"
        val DEMO_USERS = listOf(
            UserModel("1", "Mateo Estudiante", "estudiante@ucb.edu.bo", Role.ESTUDIANTE),
            UserModel("2", "Ana Staff", "staff@ucb.edu.bo", Role.STAFF),
            UserModel("3", "Admin UCB", "admin@ucb.edu.bo", Role.ADMIN)
        ).associateBy { it.email }
    }
}
