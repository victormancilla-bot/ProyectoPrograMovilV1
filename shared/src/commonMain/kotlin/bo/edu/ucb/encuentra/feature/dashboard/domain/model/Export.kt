package bo.edu.ucb.encuentra.feature.dashboard.domain.model

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.code
import bo.edu.ucb.encuentra.core.domain.model.retentionState
import bo.edu.ucb.encuentra.core.util.Csv
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Qué se exporta (D03). */
enum class ExportKind(val label: String, val fileName: String) {
    INVENTARIO("Inventario", "inventario"),
    AUDITORIA("Auditoría", "auditoria")
}

/** Lo que eligió el staff: tipo, rango de fechas y (solo inventario) estado del objeto; null = todos. */
data class ExportRequest(
    val kind: ExportKind,
    val range: DateRange,
    val status: ItemStatus? = null
)

/** Datos de origen: objetos con el nombre de quién los registró, y la auditoría ya legible (S05). */
data class ExportData(
    val items: List<FoundItem>,
    val userNames: Map<String, String>,
    val audit: List<AuditEntry>
)

/** Archivo listo para compartir. */
data class ExportFile(val name: String, val content: String, val rows: Int)

/** Arma el CSV. Puro: lo prueban los tests sin archivos ni Android. */
object ExportBuilder {

    val INVENTORY_HEADER = listOf(
        "Código", "Objeto", "Categoría", "Tipo", "Color", "Marca", "Zona", "Lugar exacto", "Encontrado",
        "Estado", "Retención", "Vence", "Custodia", "Detalle de custodia", "Registrado por"
    )
    val AUDIT_HEADER = listOf("Fecha", "Hora", "Acción", "Persona", "Sobre", "Detalle")

    /** Filas que saldrían con estos filtros (para el contador de la pantalla). */
    fun count(data: ExportData, request: ExportRequest, timeZone: TimeZone): Int = when (request.kind) {
        ExportKind.INVENTARIO -> inventory(data, request, timeZone).size
        ExportKind.AUDITORIA -> audit(data, request, timeZone).size
    }

    fun build(data: ExportData, request: ExportRequest, now: Instant, timeZone: TimeZone): ExportFile {
        val rows = when (request.kind) {
            ExportKind.INVENTARIO -> inventory(data, request, timeZone).map { item ->
                listOf(
                    item.code, item.title, item.category.label, item.subcategory.label, item.color.orEmpty(), item.brand.orEmpty(),
                    item.foundZone.label, item.foundDetail.orEmpty(), dateTime(item.foundAt, timeZone), item.status.label,
                    if (item.status == ItemStatus.DISPONIBLE) item.retentionState(now).label else "",
                    date(item.retentionUntil, timeZone), item.custodyPlace.label, item.custodyDetail,
                    data.userNames[item.registeredBy] ?: item.registeredBy
                )
            }
            ExportKind.AUDITORIA -> audit(data, request, timeZone).map { entry ->
                val local = entry.at.toLocalDateTime(timeZone)
                listOf(dmy(local), hm(local), entry.action.label, entry.actorName, entry.targetLabel, entry.detail)
            }
        }
        val header = if (request.kind == ExportKind.INVENTARIO) INVENTORY_HEADER else AUDIT_HEADER
        val today = now.toLocalDateTime(timeZone).date
        return ExportFile("${request.kind.fileName}_$today.csv", Csv.build(header, rows), rows.size)
    }

    private fun inventory(data: ExportData, request: ExportRequest, tz: TimeZone): List<FoundItem> =
        data.items.filter { inRange(it.foundAt, request.range, tz) && (request.status == null || it.status == request.status) }
            .sortedByDescending { it.foundAt }

    private fun audit(data: ExportData, request: ExportRequest, tz: TimeZone): List<AuditEntry> =
        data.audit.filter { inRange(it.at, request.range, tz) }.sortedByDescending { it.at }

    private fun inRange(at: Instant, range: DateRange, tz: TimeZone): Boolean {
        val start = range.from?.atStartOfDayIn(tz)
        val end = range.to?.plus(DatePeriod(days = 1))?.atStartOfDayIn(tz)
        return (start == null || at >= start) && (end == null || at < end)
    }

    private fun dmy(t: LocalDateTime) = "${t.day.pad()}/${t.month.ordinal.plus(1).pad()}/${t.year}"
    private fun hm(t: LocalDateTime) = "${t.hour.pad()}:${t.minute.pad()}"
    private fun date(at: Instant, tz: TimeZone) = dmy(at.toLocalDateTime(tz))
    private fun dateTime(at: Instant, tz: TimeZone) = at.toLocalDateTime(tz).let { "${dmy(it)} ${hm(it)}" }
    private fun Int.pad() = toString().padStart(2, '0')
}
