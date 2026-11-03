package bo.edu.ucb.encuentra.feature.matching.data

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
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore

class RoomMatchingStore(
    private val foundItemDao: FoundItemDao,
    private val lostReportDao: LostReportDao,
    private val matchDao: MatchDao,
    /** N01: guarda en Room y avisa en el sistema si el dueño del reporte es quien usa la app. */
    private val notifications: NotificationCenter
) : MatchingStore {

    override suspend fun availableFoundItems(): List<FoundItem> =
        foundItemDao.getByStatus(ItemStatus.DISPONIBLE).map { it.toModel() }

    override suspend fun searchingReports(): List<LostReport> =
        lostReportDao.getByStatus(ReportStatus.BUSCANDO).map { it.toModel() }

    override suspend fun matchesForReport(reportId: ItemId): List<Match> =
        matchDao.getByLostReport(reportId.value).map { it.toModel() }

    override suspend fun saveMatches(matches: List<Match>) {
        if (matches.isNotEmpty()) matchDao.upsertAll(matches.map { it.toEntity() })
    }

    override suspend fun deleteMatches(ids: List<ItemId>) {
        if (ids.isNotEmpty()) matchDao.deleteByIds(ids.map { it.value })
    }

    override suspend fun notify(notifications: List<AppNotification>) = this.notifications.publish(notifications)
}
