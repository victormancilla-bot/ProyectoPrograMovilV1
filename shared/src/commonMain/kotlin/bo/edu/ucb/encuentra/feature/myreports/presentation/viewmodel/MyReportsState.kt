package bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportDetail
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportFilter

/**
 * Mis reportes (L05). La lista y el detalle viven en la misma pantalla: [selectedReportId] abre el detalle
 * (así no se agrega una ruta a `NavRoute.kt`, que está congelado); atrás vuelve a la lista.
 */
data class MyReportsState(
    val loading: Boolean = true,
    val error: String? = null,
    val reports: List<LostReport> = emptyList(),
    val filter: ReportFilter = ReportFilter.TODOS,
    val selectedReportId: ItemId? = null,
    val detail: ReportDetail? = null,
    val showCloseDialog: Boolean = false,
    val closing: Boolean = false,
    val closeError: String? = null
) {
    val visibleReports: List<LostReport> get() = reports.filter { filter.accepts(it.status) }

    /** El usuario no tiene ningún reporte (no confundir con un filtro sin resultados). */
    val isEmpty: Boolean get() = !loading && error == null && reports.isEmpty()

    val isDetailOpen: Boolean get() = selectedReportId != null

    /** Solo un reporte BUSCANDO se cierra desde aquí; los demás los cierra el flujo de reclamo. */
    val canClose: Boolean get() = detail?.report?.status == ReportStatus.BUSCANDO
}
