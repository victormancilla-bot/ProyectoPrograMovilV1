package bo.edu.ucb.encuentra.feature.dashboard.data

import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Instant

class DashboardRepositoryImpl(
    private val realtime: RealtimeStatsDataSource,
    private val foundItemDao: FoundItemDao,
    private val claimDao: ClaimDao,
    private val userDao: UserDao
) : DashboardRepository {

    override fun observeRealtimeStats(): Flow<DashboardStats?> = realtime.observe()

    /** Cuatro consultas COUNT de Room combinadas: cualquier cambio en objetos o reclamos actualiza el panel. */
    override fun observeLocalStats(expiringBefore: Instant): Flow<DashboardStats> = combine(
        foundItemDao.observeInCustodyCount(),
        claimDao.observeCountByStatus(ClaimStatus.APROBADO),
        claimDao.observeCountByStatus(ClaimStatus.EN_VERIFICACION),
        foundItemDao.observeExpiringCount(expiringBefore.toEpochMilliseconds())
    ) { inCustody, pendingDelivery, inVerification, expiring ->
        DashboardStats(inCustody, pendingDelivery, inVerification, expiring)
    }

    override fun observeLatestPendingClaims(limit: Int): Flow<List<PendingClaim>> =
        combine(claimDao.observeLatestOpen(limit), foundItemDao.observeAll()) { claims, items ->
            val itemsById = items.associateBy { it.id }
            claims.mapNotNull { claim ->
                itemsById[claim.foundItemId]?.let { item ->
                    PendingClaim(
                        claimId = ItemId(claim.id),
                        number = claim.number,
                        itemTitle = item.title,
                        category = item.category,
                        claimantName = userDao.getById(claim.claimantId)?.fullName ?: claim.claimantId,
                        status = claim.status,
                        createdAt = Instant.fromEpochMilliseconds(claim.createdAt)
                    )
                }
            }
        }
}
