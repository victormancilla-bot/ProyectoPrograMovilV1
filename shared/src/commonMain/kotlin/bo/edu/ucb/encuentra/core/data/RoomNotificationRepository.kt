package bo.edu.ucb.encuentra.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first

/**
 * Notificaciones en Room y, en la DataStore de la sesión (S01), hasta cuándo ya se mostraron en el sistema a
 * cada usuario de este dispositivo (`notified_until_<userId>`).
 */
class RoomNotificationRepository(
    private val dao: AppNotificationDao,
    private val dataStore: DataStore<Preferences>
) : NotificationRepository {

    override suspend fun save(notifications: List<AppNotification>) = dao.upsertAll(notifications.map { it.toEntity() })

    override suspend fun unreadSince(userId: String, sinceMillis: Long): List<AppNotification> =
        dao.getUnreadSince(userId, sinceMillis).map { it.toModel() }

    override suspend fun shownUntil(userId: String): Long = dataStore.data.first()[key(userId)] ?: 0L

    override suspend fun setShownUntil(userId: String, millis: Long) {
        dataStore.edit { it[key(userId)] = millis }
    }

    private fun key(userId: String) = longPreferencesKey("notified_until_$userId")
}
