package bo.edu.ucb.encuentra.feature.login.domain.usecase

import bo.edu.ucb.encuentra.core.domain.vo.Email

/** Paso 1 del login: el correo debe ser institucional (@ucb.edu.bo) antes de pedir la contraseña. */
class ValidateInstitutionalEmailUseCase {
    operator fun invoke(raw: String): Result<Email> = when {
        raw.isBlank() -> Result.failure(IllegalArgumentException(EMPTY))
        !Email(raw).isInstitutional() -> Result.failure(IllegalArgumentException(NOT_INSTITUTIONAL))
        else -> Result.success(Email(raw.trim()))
    }

    companion object {
        const val EMPTY = "Ingresa tu correo institucional"
        const val NOT_INSTITUTIONAL = "Usa tu correo institucional @ucb.edu.bo"
    }
}
