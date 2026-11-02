package bo.edu.ucb.encuentra.feature.matching.domain.engine

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Matching fase 1 (M01): filtro duro + score básico, sin IA y sin dependencias de UI ni de Room.
 *
 * 1. **Filtro duro** (descarta el ruido): misma sede, misma categoría y subcategoría, objeto DISPONIBLE,
 *    reporte BUSCANDO y fechas compatibles (encontrado desde el inicio del rango de pérdida hasta
 *    [MAX_DAYS_TO_FIND] días después del final).
 * 2. **Score 0–100**: color (40) + marca (25) + zona (25) + cercanía en fechas (10).
 *    Lo que no se sabe (sin color, sin marca, "Otro lugar") suma la mitad: ni confirma ni descarta.
 */
object MatchEngine {

    const val COLOR_POINTS = 40
    const val BRAND_POINTS = 25
    const val ZONE_POINTS = 25
    const val DATE_POINTS = 10

    /** Un objeto puede aparecer hasta 30 días después de perderse (luego pasa a otra fase de búsqueda). */
    val MAX_DAYS_TO_FIND: Duration = 30.days

    /** Debajo de este score no se guarda la coincidencia: pasó el filtro pero casi nada coincide. */
    const val MIN_SCORE = 30

    /** ¿Pasa el filtro duro? */
    fun isCandidate(report: LostReport, item: FoundItem): Boolean =
        report.status == ReportStatus.BUSCANDO &&
            item.status == ItemStatus.DISPONIBLE &&
            report.campus == item.campus &&
            report.category == item.category &&
            report.subcategory == item.subcategory &&
            item.foundAt >= report.lostFrom &&
            item.foundAt <= report.lostTo + MAX_DAYS_TO_FIND

    /** Score 0–100 de un par que ya pasó el filtro duro. */
    fun score(report: LostReport, item: FoundItem): Int =
        colorPoints(report.color, item.color) +
            brandPoints(report.brand, item.brand) +
            zonePoints(report.zone, item.foundZone) +
            datePoints(report, item)

    /** Pares que pasan el filtro con su score, de mayor a menor; los que no llegan a [MIN_SCORE] se descartan. */
    fun rank(report: LostReport, items: List<FoundItem>): List<Pair<FoundItem, Int>> =
        items.filter { isCandidate(report, it) }
            .map { it to score(report, it) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }

    /** Al registrar un hallazgo: qué reportes podrían ser de ese objeto. */
    fun rank(item: FoundItem, reports: List<LostReport>): List<Pair<LostReport, Int>> =
        reports.filter { isCandidate(it, item) }
            .map { it to score(it, item) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }

    private fun colorPoints(lost: String?, found: String?): Int = when {
        lost.isNullOrBlank() || found.isNullOrBlank() -> COLOR_POINTS / 2
        lost.normalized() == found.normalized() -> COLOR_POINTS
        else -> 0
    }

    private fun brandPoints(lost: String?, found: String?): Int = when {
        lost.isNullOrBlank() || found.isNullOrBlank() -> BRAND_POINTS / 2
        lost.normalized() == found.normalized() -> BRAND_POINTS
        // "Casio fx-991" y "Casio" cuentan como la misma marca.
        lost.normalized().contains(found.normalized()) || found.normalized().contains(lost.normalized()) -> BRAND_POINTS
        else -> 0
    }

    private fun zonePoints(lost: CampusZone, found: CampusZone): Int = when {
        lost == found -> ZONE_POINTS
        lost == CampusZone.OTRO || found == CampusZone.OTRO -> ZONE_POINTS / 2
        else -> 0
    }

    /** Encontrado dentro del rango de pérdida: puntaje completo; después, baja un punto cada 3 días. */
    private fun datePoints(report: LostReport, item: FoundItem): Int {
        if (item.foundAt <= report.lostTo) return DATE_POINTS
        val daysLate = (item.foundAt - report.lostTo).inWholeDays.toInt()
        return (DATE_POINTS - daysLate / 3).coerceAtLeast(0)
    }

    private fun String.normalized() = trim().lowercase()
}
