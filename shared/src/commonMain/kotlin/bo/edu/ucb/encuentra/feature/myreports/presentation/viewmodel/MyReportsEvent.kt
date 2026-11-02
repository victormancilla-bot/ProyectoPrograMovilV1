package bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportFilter

sealed class MyReportsEvent {
    data class OnFilterSelected(val filter: ReportFilter) : MyReportsEvent()
    data class OnOpenReport(val reportId: ItemId) : MyReportsEvent()
    /** Flecha atrás o botón atrás del teléfono estando en el detalle. */
    data object OnBackFromDetail : MyReportsEvent()

    // Cerrar reporte (con confirmación)
    data object OnCloseReportClicked : MyReportsEvent()
    data class OnConfirmClose(val reason: CloseReason) : MyReportsEvent()
    data object OnDismissClose : MyReportsEvent()

    data class OnMatchClicked(val matchId: ItemId) : MyReportsEvent()
    data object OnSeeAllMatches : MyReportsEvent()
    /** Estado vacío: todavía no reportó nada → wizard. */
    data object OnReportLost : MyReportsEvent()
    data object OnRetry : MyReportsEvent()
}
