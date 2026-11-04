package bo.edu.ucb.encuentra.feature.dashboard.data

import bo.edu.ucb.encuentra.core.data.local.dao.AuditLogDao
import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ItemOutcome
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Instant

/** Objetos con su salida de custodia: la entrega sale del reclamo (V04); donar/descartar, de la auditoría (D04). */
class MetricsRepositoryImpl(
    private val foundItemDao: FoundItemDao,
    private val claimDao: ClaimDao,
    private val auditLogDao: AuditLogDao
) : MetricsRepository {

    override fun observeOutcomes(): Flow<List<ItemOutcome>> = combine(
        foundItemDao.observeAll(),
        claimDao.observeAll(),
        auditLogDao.observeAll()
    ) { items, claims, logs ->
        outcomesOf(items.map { it.toModel() }, claims.map { it.toModel() }, logs.map { it.toModel() })
    }
}

internal fun outcomesOf(items: List<FoundItem>, claims: List<Claim>, logs: List<AuditLog>): List<ItemOutcome> {
    val deliveredAt: Map<String, Instant> = claims.mapNotNull { c -> c.deliveredAt?.let { c.foundItemId.value to it } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, dates) -> dates.max() }
    val exitLogs: Map<String, Instant> = logs.filter { it.action in EXIT_ACTIONS }
        .groupBy { it.targetId }.mapValues { (_, list) -> list.maxOf { it.at } }
    return items.map { item ->
        val id = item.id.value
        val exit = when (item.status) {
            ItemStatus.ENTREGADO -> deliveredAt[id] ?: exitLogs[id]
            ItemStatus.DONADO, ItemStatus.DESCARTADO -> exitLogs[id]
            else -> null
        }
        ItemOutcome(item, exit)
    }
}

private val EXIT_ACTIONS = setOf(AuditAction.ITEM_DELIVERED, AuditAction.ITEM_DONATED, AuditAction.ITEM_DISCARDED)
