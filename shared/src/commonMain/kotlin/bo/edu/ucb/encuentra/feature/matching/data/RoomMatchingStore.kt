package bo.edu.ucb.encuentra.feature.matching.data

import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore

class RoomMatchingStore(
    private val foundItemDao: FoundItemDao,
    private val lostReportDao: LostReportDao,
    private val matchDao: MatchDao,
    private val notificationDao: AppNotificationDao
) : MatchingStore {

    override suspend fun availableFoundItems(): List<FoundItem> =
        foundItemDao.getByStatus(ItemStatus.DISPONIBLE).map { it.toModel() }

    override suspend fun searchingReports(): List<LostReport> =
        lostReportDao.getByStatus(ReportStatus.BUSCANDO).map { it.toModel() }

    override suspend fun saveNewMatches(matches: List<Match>): List<Match> {
        val new = matches.filter { matchDao.getByPair(it.lostReportId.value, it.foundItemId.value) == null }
        matchDao.upsertAll(new.map { it.toEntity() })
        return new
    }

    override suspend fun notify(notifications: List<AppNotification>) =
        notificationDao.upsertAll(notifications.map { it.toEntity() })
}
