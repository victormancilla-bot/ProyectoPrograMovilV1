package bo.edu.ucb.encuentra.feature.claims.data

import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimWithItem
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ClaimRepositoryImpl(
    private val claimDao: ClaimDao,
    private val matchDao: MatchDao,
    private val lostReportDao: LostReportDao,
    private val foundItemDao: FoundItemDao,
    private val userDao: UserDao
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWithItem(claimId: ItemId): Flow<ClaimWithItem?> =
        claimDao.observeById(claimId.value).flatMapLatest { claim ->
            if (claim == null) {
                flowOf(null)
            } else {
                foundItemDao.observeById(claim.foundItemId).map { item ->
                    item?.let { ClaimWithItem(claim.toModel(), it.toModel(), userDao.getById(claim.claimantId)?.fullName) }
                }
            }
        }

    override fun observeOpen(): Flow<List<ClaimWithItem>> =
        combine(claimDao.observeOpen(), foundItemDao.observeAll()) { claims, items ->
            val itemsById = items.associateBy { it.id }
            claims.mapNotNull { claim ->
                itemsById[claim.foundItemId]?.let {
                    ClaimWithItem(claim.toModel(), it.toModel(), userDao.getById(claim.claimantId)?.fullName)
                }
            }
        }
}
