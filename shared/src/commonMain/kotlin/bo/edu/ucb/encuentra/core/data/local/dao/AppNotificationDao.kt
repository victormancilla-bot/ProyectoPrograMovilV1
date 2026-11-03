package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.AppNotificationEntity
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeByUser(userId: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    /** "Coincidencias nuevas" del Home (L01): notificaciones de un tipo que el usuario todavía no leyó. */
    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND type = :type AND read = 0")
    fun observeUnreadCountByType(userId: String, type: NotificationType): Flow<Int>

    @Upsert
    suspend fun upsert(notification: AppNotificationEntity)

    @Upsert
    suspend fun upsertAll(notifications: List<AppNotificationEntity>)

    /** N01: no leídas creadas después de [since] (las que todavía no se mostraron en el sistema). */
    @Query("SELECT * FROM notifications WHERE user_id = :userId AND read = 0 AND created_at > :since ORDER BY created_at")
    suspend fun getUnreadSince(userId: String, since: Long): List<AppNotificationEntity>

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}
