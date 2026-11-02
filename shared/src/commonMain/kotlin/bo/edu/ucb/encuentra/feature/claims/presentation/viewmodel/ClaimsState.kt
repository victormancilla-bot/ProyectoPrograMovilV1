package bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview

/** Lista mínima de reclamos por revisar (V01). V03 la reemplaza por el diseño del mockup. */
data class ClaimsState(
    val loading: Boolean = true,
    val error: String? = null,
    val claims: List<ClaimReview> = emptyList()
)

sealed class ClaimsEvent {
    data class OnClaimClicked(val claimId: String) : ClaimsEvent()
    data object OnRetry : ClaimsEvent()
}

sealed class ClaimsEffect {
    data class NavigateToDetail(val claimId: String) : ClaimsEffect()
}
