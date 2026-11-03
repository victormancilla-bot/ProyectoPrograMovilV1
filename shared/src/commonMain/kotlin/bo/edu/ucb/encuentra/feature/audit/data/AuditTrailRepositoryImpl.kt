package bo.edu.ucb.encuentra.feature.audit.data

import bo.edu.ucb.encuentra.core.data.local.dao.AuditLogDao
import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.code
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditTargetKind
import bo.edu.ucb.encuentra.feature.audit.domain.repository.AuditTrailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AuditTrailRepositoryImpl(
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao,
    private val foundItemDao: FoundItemDao,
    private val claimDao: ClaimDao,
    private val lostReportDao: LostReportDao
) : AuditTrailRepository {

    override fun observeEntries(): Flow<List<AuditEntry>> = combine(
        auditLogDao.observeAll(),
        userDao.observeAll(),
        foundItemDao.observeAll(),
        claimDao.observeAll(),
        lostReportDao.observeAll()
    ) { logs, users, items, claims, reports ->
        val labels = AuditLabels(
            users = users.associate { it.id to it.fullName },
            items = items.associate { it.id to it.toModel() },
            claims = claims.associate { it.id to it.toModel() },
            reports = reports.associate { it.id to it.toModel() }
        )
        logs.map { labels.entryFor(it.toModel()) }
    }
}

/** Traduce ids a nombres legibles. Si algo ya no existe, se muestra el id (la auditoría nunca se pierde). */
internal class AuditLabels(
    private val users: Map<String, String>,
    private val items: Map<String, FoundItem>,
    private val claims: Map<String, Claim>,
    private val reports: Map<String, LostReport>
) {
    fun entryFor(log: AuditLog): AuditEntry {
        val kind = AuditTargetKind.of(log.targetId)
        val claim = claims[log.targetId]
        val itemId = when (kind) {
            AuditTargetKind.OBJETO -> log.targetId
            AuditTargetKind.RECLAMO -> claim?.foundItemId?.value
            else -> null
        }
        val label = when (kind) {
            AuditTargetKind.OBJETO -> items[log.targetId]?.let { "${it.title} (${it.code})" }
            AuditTargetKind.RECLAMO -> claim?.let { c ->
                listOfNotNull("Reclamo #${c.number}", items[c.foundItemId.value]?.title).joinToString(" · ")
            }
            AuditTargetKind.PERSONA -> users[log.targetId]
            AuditTargetKind.REPORTE -> reports[log.targetId]?.let { "Reporte: ${it.title}" }
            AuditTargetKind.OTRO -> null
        } ?: log.targetId
        return AuditEntry(
            id = log.id,
            action = log.action,
            at = log.at,
            actorId = log.actorId,
            actorName = users[log.actorId] ?: log.actorId,
            targetId = log.targetId,
            targetKind = kind,
            targetLabel = label,
            // El código del objeto también encuentra sus reclamos ("#LLAVERO").
            itemKey = itemId?.let { id -> listOfNotNull(id, items[id]?.code).joinToString(" ") },
            detail = log.detail
        )
    }
}
