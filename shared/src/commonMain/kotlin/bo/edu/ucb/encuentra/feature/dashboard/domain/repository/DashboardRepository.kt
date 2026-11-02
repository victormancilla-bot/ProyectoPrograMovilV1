package bo.edu.ucb.encuentra.feature.dashboard.domain.repository

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface DashboardRepository {
    /**
     * Contadores publicados en Firebase Realtime Database (`stats/`), en vivo. Emite null si el nodo no existe
     * o está incompleto; falla si Firebase no está configurado (todavía no hay `google-services.json`).
     */
    fun observeRealtimeStats(): Flow<DashboardStats?>

    /** Los mismos contadores calculados desde Room, en vivo. "Por vencer" = vencen antes de [expiringBefore]. */
    fun observeLocalStats(expiringBefore: Instant): Flow<DashboardStats>

    /** Los [limit] reclamos abiertos más recientes. */
    fun observeLatestPendingClaims(limit: Int): Flow<List<PendingClaim>>
}
