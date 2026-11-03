package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {
    // Los correos se guardan en minúsculas: "Ana.Gomez@UCB.edu.bo " encuentra a ana.gomez@ucb.edu.bo.
    override suspend fun findByEmail(email: Email): UserModel? =
        userDao.getByEmail(email.value.trim().lowercase())?.toModel()

    override suspend fun findById(id: String): UserModel? = userDao.getById(id)?.toModel()

    override fun observeAll(): Flow<List<UserModel>> = userDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun updateRole(id: String, role: Role) {
        val user = userDao.getById(id) ?: return
        userDao.upsert(user.copy(role = role))
    }
}
