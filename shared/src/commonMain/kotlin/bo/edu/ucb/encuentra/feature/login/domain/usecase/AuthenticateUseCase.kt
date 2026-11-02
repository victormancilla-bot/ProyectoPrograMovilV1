package bo.edu.ucb.encuentra.feature.login.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.Password
import kotlinx.coroutines.delay

/**
 * Login de prueba hasta S01/S02 (Firebase Auth). Si las credenciales son correctas guarda la sesión.
 */
class AuthenticateUseCase(
    private val session: SessionRepository
) {
    suspend operator fun invoke(email: Email, password: Password): Result<UserModel> {
        delay(FAKE_NETWORK_DELAY_MS)
        val user = DEMO_USERS[email.value.trim().lowercase()]
        return if (user != null && password.value == DEMO_PASSWORD) {
            session.setCurrentUser(user)
            Result.success(user)
        } else {
            Result.failure(Exception("Credenciales incorrectas"))
        }
    }

    companion object {
        const val DEMO_PASSWORD = "password"
        private const val FAKE_NETWORK_DELAY_MS = 1000L
        // Mismos ids que el seed de Room (SeedData), así el estudiante logueado es dueño de los reportes de prueba.
        val DEMO_USERS = listOf(
            UserModel("u-mateo", "Mateo Quispe", Email("estudiante@ucb.edu.bo"), Role.ESTUDIANTE, Campus.LA_PAZ, "Ingeniería"),
            UserModel("u-ana-staff", "Ana Staff", Email("staff@ucb.edu.bo"), Role.STAFF, Campus.LA_PAZ, "Objetos Perdidos"),
            UserModel("u-admin", "Admin UCB", Email("admin@ucb.edu.bo"), Role.ADMIN, Campus.LA_PAZ, "Administración")
        ).associateBy { it.email.value }
    }
}
