package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.formatShortRange
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import kotlin.time.Duration.Companion.days

/**
 * Una fila de "Reporte del estudiante vs Hallazgo" (V03). [matches] = ✔ / ✘, o null si no aplica o falta un dato.
 * A diferencia del Detalle de match del estudiante (M05), el staff ve TODO: descripción y lugar exacto incluidos.
 */
internal data class ComparisonLine(val label: String, val reported: String, val found: String, val matches: Boolean?)

private const val NOT_GIVEN = "No indicado"

/** Días después de la pérdida en que un hallazgo todavía cuenta como "fecha compatible" (como el filtro de M01). */
private const val DAYS_TO_FIND = 30

internal fun comparisonLines(report: LostReport, item: FoundItem): List<ComparisonLine> = listOf(
    ComparisonLine("Objeto", report.title, item.title, null),
    ComparisonLine("Tipo", report.subcategory.label, item.subcategory.label, report.subcategory == item.subcategory),
    ComparisonLine("Marca", report.brand ?: NOT_GIVEN, item.brand ?: NOT_GIVEN, sameBrand(report.brand, item.brand)),
    ComparisonLine("Color", report.color ?: NOT_GIVEN, item.color ?: NOT_GIVEN, sameText(report.color, item.color)),
    ComparisonLine(
        "Lugar",
        listOfNotNull(report.zone.label, report.locationDetail).joinToString(" · "),
        listOfNotNull(item.foundZone.label, item.foundDetail).joinToString(" · "),
        report.zone == item.foundZone
    ),
    ComparisonLine(
        "Fecha",
        formatShortRange(report.lostFrom.toLocalDate(), report.lostTo.toLocalDate()),
        item.foundAt.toLocalDate().formatShort(),
        item.foundAt >= report.lostFrom && item.foundAt <= report.lostTo + DAYS_TO_FIND.days
    ),
    ComparisonLine("Descripción", report.description.ifBlank { NOT_GIVEN }, item.description.ifBlank { NOT_GIVEN }, null)
)

private fun sameText(a: String?, b: String?): Boolean? =
    if (a.isNullOrBlank() || b.isNullOrBlank()) null else a.trim().equals(b.trim(), ignoreCase = true)

/** "Casio fx-991" y "Casio" cuentan como la misma marca (igual que el matching). */
private fun sameBrand(a: String?, b: String?): Boolean? {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return null
    val x = a.trim().lowercase()
    val y = b.trim().lowercase()
    return x.contains(y) || y.contains(x)
}
