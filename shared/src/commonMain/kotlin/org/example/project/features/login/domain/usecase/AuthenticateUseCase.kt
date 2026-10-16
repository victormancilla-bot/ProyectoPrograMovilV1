package org.example.project.features.login.domain.usecase

import kotlinx.coroutines.delay
import org.example.project.features.login.domain.vo.Email
import org.example.project.features.login.domain.vo.Password

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
