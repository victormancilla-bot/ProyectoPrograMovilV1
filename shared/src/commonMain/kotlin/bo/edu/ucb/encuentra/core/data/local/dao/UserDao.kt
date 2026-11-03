package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    /** S03: todas las cuentas para el panel de roles del admin. */
    @Query("SELECT * FROM users ORDER BY full_name")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)
}
