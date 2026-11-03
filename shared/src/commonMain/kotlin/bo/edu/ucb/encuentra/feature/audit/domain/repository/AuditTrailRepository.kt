package bo.edu.ucb.encuentra.feature.audit.domain.repository

import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import kotlinx.coroutines.flow.Flow

/** Lectura de la auditoría (append-only: aquí no hay forma de cambiarla). Del más reciente al más antiguo. */
interface AuditTrailRepository {
    fun observeEntries(): Flow<List<AuditEntry>>
}
