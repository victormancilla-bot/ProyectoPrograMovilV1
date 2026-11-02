package bo.edu.ucb.encuentra.feature.myreports.data

import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportMatchSummary
import bo.edu.ucb.encuentra.feature.myreports.domain.repository.MyReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MyReportsRepositoryImpl(
    private val lostReportDao: LostReportDao,
    private val matchDao: MatchDao,
    private val foundItemDao: FoundItemDao
) : MyReportsRepository {

    override fun observeByOwner(ownerId: String): Flow<List<LostReport>> =
        lostReportDao.observeByOwner(ownerId).map { reports -> reports.map { it.toModel() } }

    override fun observeReport(reportId: ItemId): Flow<LostReport?> =
        lostReportDao.observeById(reportId.value).map { it?.toModel() }

    override fun observeMatches(reportId: ItemId): Flow<List<ReportMatchSummary>> = combine(
        matchDao.observeActiveByLostReport(reportId.value),
        foundItemDao.observeAll()
    ) { matches, items ->
        val itemsById = items.associateBy { it.id }
        matches.mapNotNull { match ->
            itemsById[match.foundItemId]?.let { item ->
                ReportMatchSummary(
                    matchId = ItemId(match.id),
                    itemTitle = item.title,
                    itemCategory = item.category,
                    foundAt = item.toModel().foundAt,
                    score = match.score,
                    status = match.status
                )
            }
        }
    }

    override suspend fun getReport(reportId: ItemId): LostReport? = lostReportDao.getById(reportId.value)?.toModel()

    override suspend fun updateStatus(reportId: ItemId, status: ReportStatus) =
        lostReportDao.updateStatus(reportId.value, status)
}
