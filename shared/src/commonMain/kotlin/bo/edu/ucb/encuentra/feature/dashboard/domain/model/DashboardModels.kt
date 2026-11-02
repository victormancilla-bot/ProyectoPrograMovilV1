package bo.edu.ucb.encuentra.feature.dashboard.domain.model

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Instant

/** Los 4 contadores del Dashboard Administrador (mockup "Dashboard Admin"). */
data class DashboardStats(
    /** Objetos guardados en Objetos Perdidos: disponibles o reclamados. */
    val inCustody: Int,
    /** Reclamos aprobados que esperan que el dueño recoja el objeto. */
    val pendingDelivery: Int,
    /** Reclamos con las preguntas de control respondidas, esperando la decisión del staff. */
    val inVerification: Int,
    /** Objetos disponibles cuya retención vence en los próximos [EXPIRING_WINDOW_DAYS] días. */
    val expiringSoon: Int
) {
    companion object {
        const val EXPIRING_WINDOW_DAYS = 7
    }
}

/** De dónde salen los contadores: Firebase Realtime Database (`stats/`) o, si no está disponible, Room. */
enum class StatsSource { REALTIME_DATABASE, LOCAL }

data class LiveStats(val stats: DashboardStats, val source: StatsSource)

/** Una fila de "Últimos reclamos pendientes". */
data class PendingClaim(
    val claimId: ItemId,
    val number: Int,
    val itemTitle: String,
    val category: Category,
    val claimantName: String,
    val status: ClaimStatus,
    val createdAt: Instant
)
