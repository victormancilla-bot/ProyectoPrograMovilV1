package bo.edu.ucb.encuentra.core.data.local.seed

import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.dao.AuditLogDao
import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.CustodyEventDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity

/** Carga [SeedData] una sola vez: solo si la tabla de usuarios está vacía (primera ejecución). */
@Suppress("LongParameterList")
class DatabaseSeeder(
    private val userDao: UserDao,
    private val lostReportDao: LostReportDao,
    private val foundItemDao: FoundItemDao,
    private val custodyEventDao: CustodyEventDao,
    private val matchDao: MatchDao,
    private val claimDao: ClaimDao,
    private val auditLogDao: AuditLogDao,
    private val appNotificationDao: AppNotificationDao
) {
    suspend fun seedIfEmpty() {
        if (userDao.count() > 0) return
        userDao.upsertAll(SeedData.users.map { it.toEntity() })
        lostReportDao.upsertAll(SeedData.lostReports.map { it.toEntity() })
        foundItemDao.upsertAll(SeedData.foundItems.map { it.toEntity() })
        custodyEventDao.insertAll(SeedData.custodyEvents.map { it.toEntity() })
        matchDao.upsertAll(SeedData.matches.map { it.toEntity() })
        claimDao.upsertAll(SeedData.claims.map { it.toEntity() })
        auditLogDao.insertAll(SeedData.auditLogs.map { it.toEntity() })
        appNotificationDao.upsertAll(SeedData.notifications.map { it.toEntity() })
    }
}
