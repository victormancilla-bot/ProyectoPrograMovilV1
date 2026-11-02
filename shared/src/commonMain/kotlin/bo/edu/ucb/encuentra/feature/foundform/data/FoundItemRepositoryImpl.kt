package bo.edu.ucb.encuentra.feature.foundform.data

import androidx.room3.withWriteTransaction
import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.domain.repository.FoundItemRepository

class FoundItemRepositoryImpl(private val database: AppDatabase) : FoundItemRepository {

    override suspend fun register(item: FoundItem) {
        val custody = CustodyEvent(
            id = ItemId.generate(),
            foundItemId = item.id,
            place = item.custodyPlace,
            detail = item.custodyDetail,
            changedBy = item.registeredBy,
            at = item.createdAt,
            note = "Registro inicial"
        )
        val audit = AuditLog(
            id = 0,
            action = AuditAction.ITEM_REGISTERED,
            actorId = item.registeredBy,
            targetId = item.id.value,
            detail = "${item.title} · ${item.custodyPlace.label}",
            at = item.createdAt
        )
        database.withWriteTransaction {
            database.foundItemDao().upsert(item.toEntity())
            database.custodyEventDao().insert(custody.toEntity())
            database.auditLogDao().insert(audit.toEntity())
        }
    }
}
