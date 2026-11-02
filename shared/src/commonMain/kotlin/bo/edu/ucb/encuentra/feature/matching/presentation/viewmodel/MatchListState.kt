package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches

/** Posibles coincidencias (M04): cargando → contenido / vacío / error. */
data class MatchListState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Reportes del usuario con candidatos (ya ordenados). */
    val groups: List<ReportMatches> = emptyList(),
    /** Reporte cuyos candidatos se muestran (chip seleccionado). */
    val selectedReportId: ItemId? = null
) {
    val isEmpty: Boolean get() = !loading && error == null && groups.isEmpty()

    val selected: ReportMatches? get() = groups.firstOrNull { it.report.id == selectedReportId } ?: groups.firstOrNull()
}
