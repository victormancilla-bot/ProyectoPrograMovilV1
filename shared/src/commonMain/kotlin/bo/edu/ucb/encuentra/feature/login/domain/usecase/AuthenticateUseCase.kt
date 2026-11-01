package bo.edu.ucb.encuentra.feature.login.domain.usecase

import kotlinx.coroutines.delay
import bo.edu.ucb.encuentra.feature.login.domain.vo.Email
import bo.edu.ucb.encuentra.feature.login.domain.vo.Password

class AuthenticateUseCase {
    suspend operator fun invoke(email: Email, password: Password): Result<Boolean> {
        delay(1000)
        return if (email.value == "user@example.com" && password.value == "password") {
            Result.success(true)
        } else {
            Result.failure(Exception("Credenciales incorrectas"))
        }
    }
}
