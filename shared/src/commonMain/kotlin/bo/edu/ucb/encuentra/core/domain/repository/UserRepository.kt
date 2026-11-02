package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.vo.Email

/** Usuarios registrados (por ahora en Room con el seed; S02/S03 los traerán de Firebase). */
interface UserRepository {
    suspend fun findByEmail(email: Email): UserModel?
}
