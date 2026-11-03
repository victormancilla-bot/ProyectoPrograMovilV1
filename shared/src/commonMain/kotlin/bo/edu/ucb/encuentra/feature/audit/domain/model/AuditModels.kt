package bo.edu.ucb.encuentra.feature.audit.domain.model

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Sobre qué se hizo la acción (según el id: fi- objeto, c- reclamo, u- persona, lr- reporte). */
enum class AuditTargetKind(val label: String) {
    OBJETO("Objeto"),
    RECLAMO("Reclamo"),
    PERSONA("Persona"),
    REPORTE("Reporte"),
    OTRO("Otro");

    companion object {
        fun of(targetId: String): AuditTargetKind = when {
            targetId.startsWith("fi-") -> OBJETO
            targetId.startsWith("c-") -> RECLAMO
            targetId.startsWith("u-") -> PERSONA
            targetId.startsWith("lr-") -> REPORTE
            else -> OTRO
        }
    }
}

/**
 * Un registro de auditoría listo para leer: quién (nombre), qué, sobre qué y cuándo. [itemKey] une los eventos
 * de un objeto con los de sus reclamos (id y código del objeto), así buscar un objeto muestra toda su historia.
 */
data class AuditEntry(
    val id: Long,
    val action: AuditAction,
    val at: Instant,
    val actorId: String,
    val actorName: String,
    val targetId: String,
    val targetKind: AuditTargetKind,
    val targetLabel: String,
    val itemKey: String?,
    val detail: String
) {
    internal val searchText: String =
        AuditFilter.normalize(listOf(targetLabel, targetId, itemKey.orEmpty(), detail).joinToString(" "))
}

/** Filtro por fecha del visor. */
enum class AuditPeriod(val label: String) {
    HOY("Hoy"),
    SEMANA("7 días"),
    MES("30 días"),
    TODO("Todo")
}

/** Filtros del visor: objeto (texto: nombre, código o reclamo), persona y fecha. */
data class AuditFilter(
    val query: String = "",
    val actorId: String? = null,
    val period: AuditPeriod = AuditPeriod.TODO
) {
    fun apply(entries: List<AuditEntry>, now: Instant, timeZone: TimeZone): List<AuditEntry> {
        val words = normalize(query).split(" ").filter { it.isNotBlank() }
        val today = now.toLocalDateTime(timeZone).date
        return entries.filter { entry ->
            (actorId == null || entry.actorId == actorId) &&
                words.all { it in entry.searchText } &&
                when (period) {
                    AuditPeriod.HOY -> entry.at.toLocalDateTime(timeZone).date == today
                    AuditPeriod.SEMANA -> entry.at >= now - 7.days
                    AuditPeriod.MES -> entry.at >= now - 30.days
                    AuditPeriod.TODO -> true
                }
        }
    }

    companion object {
        private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

        internal fun normalize(text: String): String =
            text.lowercase().map { ACCENTS[it] ?: it }.joinToString("").replace(Regex("[^a-z0-9ñ#]+"), " ").trim()
    }
}
