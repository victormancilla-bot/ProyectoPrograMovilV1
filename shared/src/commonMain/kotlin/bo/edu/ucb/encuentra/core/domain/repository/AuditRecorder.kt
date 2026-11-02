package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AuditAction

/**
 * Hook de auditoría: los use cases registran aquí cada cambio sensible.
 * Hoy escribe en la tabla append-only de Room; S05 puede sumarle Firestore o la pantalla de auditoría sin tocar los use cases.
 */
interface AuditRecorder {
    suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String)
}
