package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.vo.Email
import kotlinx.coroutines.flow.Flow

/** Usuarios registrados (por ahora en Room con el seed; S02 los traerá de Firebase `users/{uid}`). */
interface UserRepository {
    suspend fun findByEmail(email: Email): UserModel?

    suspend fun findById(id: String): UserModel?

    /** S03: todas las cuentas, para el panel de roles del admin. */
    fun observeAll(): Flow<List<UserModel>>

    /** S03: solo lo usa ChangeUserRoleUseCase, que valida que quien cambia sea ADMIN. */
    suspend fun updateRole(id: String, role: Role)
}
