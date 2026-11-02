package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.Claim

/**
 * Hook de notificaciones de reclamos (V03 → N01): se llama cuando la decisión sobre un reclamo cambia su estado
 * (aprobado, rechazado, bloqueado por intentos o entregado), para avisar a quien reclamó.
 * Hoy guarda una notificación interna en Room (la ve la Bandeja de Alertas, N04); N01/N02 pueden sumarle la
 * notificación del sistema o push sin tocar los use cases. Igual que [AuditRecorder] para la auditoría (S05).
 */
interface ClaimNotifier {
    suspend fun onClaimUpdated(claim: Claim, itemTitle: String)
}
