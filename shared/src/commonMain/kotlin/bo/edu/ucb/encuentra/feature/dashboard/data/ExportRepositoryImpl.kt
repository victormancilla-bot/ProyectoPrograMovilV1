package bo.edu.ucb.encuentra.feature.dashboard.data

import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.feature.audit.domain.repository.AuditTrailRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportData
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** D03 · Inventario (Room) + auditoría con nombres legibles (la misma lectura del visor de S05). */
class ExportRepositoryImpl(
    private val foundItemDao: FoundItemDao,
    private val userDao: UserDao,
    private val auditTrail: AuditTrailRepository
) : ExportRepository {

    override fun observeData(): Flow<ExportData> = combine(
        foundItemDao.observeAll(),
        userDao.observeAll(),
        auditTrail.observeEntries()
    ) { items, users, audit ->
        ExportData(
            items = items.map { it.toModel() },
            userNames = users.associate { it.id to it.fullName },
            audit = audit
        )
    }
}
