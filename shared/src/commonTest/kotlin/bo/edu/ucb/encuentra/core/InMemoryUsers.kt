package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Usuarios del seed en memoria (fake de Room para login y roles). */
internal open class InMemoryUsers(initial: List<UserModel> = SeedData.users) : UserRepository {
    val users = MutableStateFlow(initial)

    override suspend fun findByEmail(email: Email): UserModel? =
        users.value.firstOrNull { it.email.value == email.value.trim().lowercase() }

    override suspend fun findById(id: String): UserModel? = users.value.firstOrNull { it.id == id }

    override fun observeAll(): Flow<List<UserModel>> = users

    override suspend fun updateRole(id: String, role: Role) =
        users.update { list -> list.map { if (it.id == id) it.copy(role = role) else it } }
}
