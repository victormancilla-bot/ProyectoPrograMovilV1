package bo.edu.ucb.encuentra.feature.alerts.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

/** Bandeja de Alertas (N04): las AppNotification que guarda el NotificationCenter de N01. */
interface AlertsRepository {
    /** De la más reciente a la más antigua. */
    fun observe(userId: String): Flow<List<AppNotification>>

    fun observeUnreadCount(userId: String): Flow<Int>

    suspend fun markRead(id: String)

    suspend fun markAllRead(userId: String)

    suspend fun clearRead(userId: String)
}
