package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.vo.ItemId

sealed class MatchListEvent {
    data class OnReportSelected(val reportId: ItemId) : MatchListEvent()
    data class OnMatchClicked(val matchId: ItemId) : MatchListEvent()
    /** "Modificar criterios": vuelve al reporte (Mis reportes, L05). */
    data object OnModifyCriteria : MatchListEvent()
    /** Estado vacío: todavía no reportó nada → wizard. */
    data object OnReportLost : MatchListEvent()
    data object OnRetry : MatchListEvent()

    /** "No es mío" (M02): pide confirmación; al confirmar no vuelve a aparecer. */
    data class OnRejectMatch(val matchId: ItemId) : MatchListEvent()
    data object OnConfirmReject : MatchListEvent()
    data object OnDismissReject : MatchListEvent()
}
