package bo.edu.ucb.encuentra.feature.claims.data

import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository

class ClaimRepositoryImpl(
    private val claimDao: ClaimDao,
    private val matchDao: MatchDao,
    private val lostReportDao: LostReportDao
) : ClaimRepository {

    override suspend fun getById(id: ItemId): Claim? = claimDao.getById(id.value)?.toModel()

    override suspend fun getByItem(itemId: ItemId): List<Claim> = claimDao.getByFoundItem(itemId.value).map { it.toModel() }

    override suspend fun getByClaimant(claimantId: String): List<Claim> = claimDao.getByClaimant(claimantId).map { it.toModel() }

    override suspend fun nextNumber(): Int = claimDao.nextNumber()

    override suspend fun insert(claim: Claim) = claimDao.insert(claim.toEntity())

    override suspend fun update(claim: Claim) = claimDao.upsertAll(listOf(claim.toEntity()))

    override suspend fun getMatch(id: ItemId): Match? = matchDao.getById(id.value)?.toModel()

    override suspend fun updateMatchStatus(id: ItemId, status: MatchStatus) = matchDao.updateStatus(id.value, status)

    override suspend fun getReport(id: ItemId): LostReport? = lostReportDao.getById(id.value)?.toModel()

    override suspend fun updateReportStatus(id: ItemId, status: ReportStatus) = lostReportDao.updateStatus(id.value, status)
}
