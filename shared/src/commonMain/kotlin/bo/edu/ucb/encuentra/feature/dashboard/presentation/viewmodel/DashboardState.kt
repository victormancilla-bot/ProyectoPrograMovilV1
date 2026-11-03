package bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.StatsSource
import kotlin.time.Instant

/** Dashboard Administrador (D01): contadores en vivo + últimos reclamos pendientes. */
data class DashboardState(
    val loading: Boolean = true,
    val error: String? = null,
    /** "Panel — La Paz": sede del usuario. */
    val campus: String = "",
    val stats: DashboardStats? = null,
    val source: StatsSource? = null,
    val latestClaims: List<PendingClaim> = emptyList(),
    /** Para "Hace 2 horas"; se actualiza con cada cambio. */
    val now: Instant? = null
)

/** A qué parte lleva cada tarjeta de contador. */
enum class StatTarget { INVENTORY, CLAIMS, RETENTION }

sealed class DashboardEvent {
    data object OnRegisterFound : DashboardEvent()
    data object OnSeeAllClaims : DashboardEvent()
    data object OnOpenAudit : DashboardEvent()
    data class OnClaimClicked(val claimId: String) : DashboardEvent()
    data class OnStatClicked(val target: StatTarget) : DashboardEvent()
    data object OnRetry : DashboardEvent()
}

sealed class DashboardEffect {
    data object NavigateToFoundForm : DashboardEffect()
    data object NavigateToClaims : DashboardEffect()
    data object NavigateToInventory : DashboardEffect()
    data object NavigateToRetention : DashboardEffect()
    data object NavigateToAudit : DashboardEffect()
    data class NavigateToClaimDetail(val claimId: String) : DashboardEffect()
}
