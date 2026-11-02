package bo.edu.ucb.encuentra.feature.login.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.Password
import kotlinx.coroutines.delay

/**
 * Login institucional (S01): solo @ucb.edu.bo y solo usuarios registrados (seed de Room).
 * La contraseña es la de prueba [DEMO_PASSWORD] hasta que S02 ingrese con Google UCB (Firebase Auth).
 * Si todo es correcto guarda la sesión persistente.
 */
class AuthenticateUseCase(
    private val users: UserRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(email: Email, password: Password): Result<UserModel> {
        if (!email.isInstitutional()) {
            return Result.failure(IllegalArgumentException(ValidateInstitutionalEmailUseCase.NOT_INSTITUTIONAL))
        }
        delay(FAKE_NETWORK_DELAY_MS)
        val user = users.findByEmail(email)
        // Mismo mensaje para "no existe" y "contraseña incorrecta": no revela qué correos están registrados.
        if (user == null || password.value != DEMO_PASSWORD) {
            return Result.failure(IllegalArgumentException(WRONG_CREDENTIALS))
        }
        session.setCurrentUser(user)
        return Result.success(user)
    }

    companion object {
        const val DEMO_PASSWORD = "password"
        const val WRONG_CREDENTIALS = "Correo o contraseña incorrectos"
        private const val FAKE_NETWORK_DELAY_MS = 800L
    }
}
