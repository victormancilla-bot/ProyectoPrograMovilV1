package bo.edu.ucb.encuentra.feature.myreports.domain.model

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Instant

/** Reporte "en curso": el Home los cuenta como activos y se pueden cerrar desde el detalle. */
val ReportStatus.isActive: Boolean
    get() = this == ReportStatus.BUSCANDO || this == ReportStatus.EN_VERIFICACION || this == ReportStatus.MATCH_CONFIRMADO

/** Chips de Mis reportes (mockup: Todos / Buscando / Verificación). */
enum class ReportFilter(val label: String) {
    TODOS("Todos"),
    BUSCANDO("Buscando"),

    /** En verificación o con match confirmado: ya hay un objeto candidato en proceso. */
    VERIFICACION("Verificación");

    fun accepts(status: ReportStatus): Boolean = when (this) {
        TODOS -> true
        BUSCANDO -> status == ReportStatus.BUSCANDO
        VERIFICACION -> status == ReportStatus.EN_VERIFICACION || status == ReportStatus.MATCH_CONFIRMADO
    }
}

/** Una coincidencia del reporte, lista para mostrar en su detalle (sin depender de la feature de matching). */
data class ReportMatchSummary(
    val matchId: ItemId,
    val itemTitle: String,
    val itemCategory: Category,
    val foundAt: Instant,
    val score: Int,
    val status: MatchStatus
)

/** Detalle de un reporte: sus datos y sus coincidencias, de mayor a menor score. */
data class ReportDetail(val report: LostReport, val matches: List<ReportMatchSummary>)

/** Cómo se cierra un reporte desde el detalle (con confirmación). */
enum class CloseReason(val label: String, val status: ReportStatus) {
    FOUND_IT("Ya lo encontré por mi cuenta", ReportStatus.RECUPERADO),
    STOP_SEARCHING("Ya no lo estoy buscando", ReportStatus.CERRADO)
}
