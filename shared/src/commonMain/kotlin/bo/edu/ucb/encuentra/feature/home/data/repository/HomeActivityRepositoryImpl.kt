package bo.edu.ucb.encuentra.feature.home.data.repository

import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.feature.home.domain.model.HomeActivity
import bo.edu.ucb.encuentra.feature.home.domain.repository.HomeActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HomeActivityRepositoryImpl(
    private val lostReportDao: LostReportDao,
    private val notificationDao: AppNotificationDao
) : HomeActivityRepository {

    override fun observe(userId: String): Flow<HomeActivity> = combine(
        lostReportDao.observeActiveCountByOwner(userId),
        notificationDao.observeUnreadCountByType(userId, NotificationType.NEW_MATCH)
    ) { activeReports, newMatches ->
        HomeActivity(activeReports = activeReports, newMatches = newMatches)
    }
}
