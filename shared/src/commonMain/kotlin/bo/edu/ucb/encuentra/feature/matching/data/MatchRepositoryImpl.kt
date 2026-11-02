package bo.edu.ucb.encuentra.feature.matching.data

import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchCandidate
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchPair
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Junta reportes, coincidencias y objetos encontrados de Room; cualquier cambio en las 3 tablas actualiza la lista. */
class MatchRepositoryImpl(
    private val matchDao: MatchDao,
    private val lostReportDao: LostReportDao,
    private val foundItemDao: FoundItemDao,
    private val claimDao: ClaimDao
) : MatchRepository {

    override fun observeForOwner(ownerId: String): Flow<List<ReportMatches>> = combine(
        lostReportDao.observeByOwner(ownerId),
        matchDao.observeActiveByOwner(ownerId),
        foundItemDao.observeAll()
    ) { reports, matches, items ->
        val itemsById = items.associateBy { it.id }
        val matchesByReport = matches.groupBy { it.lostReportId }
        reports.map { report ->
            ReportMatches(
                report = report.toModel(),
                candidates = matchesByReport[report.id].orEmpty().mapNotNull { match ->
                    itemsById[match.foundItemId]?.let { MatchCandidate(match.toModel(), it.toModel()) }
                }
            )
        }
    }

    /** Si cambia el match, su reporte, su objeto o el reclamo del dueño (V02), el detalle se actualiza solo. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMatch(matchId: ItemId): Flow<MatchPair?> =
        matchDao.observeById(matchId.value).flatMapLatest { match ->
            if (match == null) {
                flowOf(null)
            } else {
                combine(
                    lostReportDao.observeById(match.lostReportId),
                    foundItemDao.observeById(match.foundItemId),
                    claimDao.observeByFoundItem(match.foundItemId)
                ) { report, item, claims ->
                    if (report == null || item == null) {
                        null
                    } else {
                        // Solo el reclamo del dueño del reporte: los de otras personas no se muestran.
                        val ownClaim = claims.firstOrNull { it.claimantId == report.ownerId }?.toModel()
                        MatchPair(match.toModel(), report.toModel(), item.toModel(), ownClaim)
                    }
                }
            }
        }

    override suspend fun reject(matchId: ItemId): LostReport? {
        val match = matchDao.getById(matchId.value) ?: return null
        matchDao.updateStatus(match.id, MatchStatus.DESCARTADO)
        return lostReportDao.getById(match.lostReportId)?.toModel()
    }
}
