package bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import kotlin.time.Instant

/** Panel de Verificación de Reclamos (V03, mockup "Verificación de Reclamos"): lista expandible. */
data class ClaimsState(
    val loading: Boolean = true,
    val error: String? = null,
    val claims: List<ClaimReview> = emptyList(),
    /** Tarjetas abiertas (la flecha muestra el resumen y "Revisar reclamo"). */
    val expanded: Set<String> = emptySet(),
    /** Para "Hace 2 horas". */
    val now: Instant? = null
)

sealed class ClaimsEvent {
    /** Abre o cierra el resumen de la tarjeta. */
    data class OnToggle(val claimId: String) : ClaimsEvent()
    /** "Revisar reclamo": abre el detalle. */
    data class OnClaimClicked(val claimId: String) : ClaimsEvent()
    data object OnRetry : ClaimsEvent()
}

sealed class ClaimsEffect {
    data class NavigateToDetail(val claimId: String) : ClaimsEffect()
}
