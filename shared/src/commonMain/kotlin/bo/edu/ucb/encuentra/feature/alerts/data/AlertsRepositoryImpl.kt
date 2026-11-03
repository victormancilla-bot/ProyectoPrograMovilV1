package bo.edu.ucb.encuentra.feature.alerts.data

import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.feature.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlertsRepositoryImpl(private val dao: AppNotificationDao) : AlertsRepository {
    override fun observe(userId: String): Flow<List<AppNotification>> =
        dao.observeByUser(userId).map { list -> list.map { it.toModel() } }

    override fun observeUnreadCount(userId: String): Flow<Int> = dao.observeUnreadCount(userId)

    override suspend fun markRead(id: String) = dao.markAsRead(id)

    override suspend fun markAllRead(userId: String) = dao.markAllAsRead(userId)

    override suspend fun clearRead(userId: String) = dao.deleteRead(userId)
}
