package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.data.local.dao.AuditLogDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import kotlin.time.Clock

/** Auditoría en la tabla append-only de Room. Dentro de un [RoomTransactionRunner] se guarda con el cambio o no se guarda. */
class RoomAuditRecorder(
    private val auditLogDao: AuditLogDao,
    private val clock: Clock
) : AuditRecorder {
    override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
        auditLogDao.insert(
            AuditLog(id = 0, action = action, actorId = actorId, targetId = targetId, detail = detail, at = clock.now())
                .toEntity()
        )
    }
}
