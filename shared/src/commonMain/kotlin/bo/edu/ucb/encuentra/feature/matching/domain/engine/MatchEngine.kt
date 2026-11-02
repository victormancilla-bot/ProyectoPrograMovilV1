package bo.edu.ucb.encuentra.feature.matching.domain.engine

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import kotlin.math.floor
import kotlin.time.Duration.Companion.days

/**
 * Motor de matching, sin IA y sin dependencias de UI ni de Room.
 *
 * - **Filtro duro** (M01): misma sede, categoría y subcategoría; objeto DISPONIBLE; reporte BUSCANDO;
 *   encontrado desde el inicio del rango de pérdida hasta [MatchingConfig.maxDaysToFind] días después del final.
 * - **Score ponderado 0–100** (M02): color, marca, zona, fecha y texto, cada uno con una similitud 0–1
 *   multiplicada por su peso de [MatchingConfig]. Lo que no se sabe (sin color, sin marca, "Otro lugar",
 *   sin descripción) vale 0.5: ni confirma ni descarta.
 * - **Ranking** (M02): los [MatchingConfig.topN] mejores sobre [MatchingConfig.minScore], sin los descartados.
 */
object MatchEngine {

    /** ¿Pasa el filtro duro? */
    fun isCandidate(report: LostReport, item: FoundItem, config: MatchingConfig = MatchingConfig.DEFAULT): Boolean =
        report.status == ReportStatus.BUSCANDO &&
            item.status == ItemStatus.DISPONIBLE &&
            report.campus == item.campus &&
            report.category == item.category &&
            report.subcategory == item.subcategory &&
            item.foundAt >= report.lostFrom &&
            item.foundAt <= report.lostTo + config.maxDaysToFind.days

    /** Score 0–100 de un par que ya pasó el filtro duro. */
    fun score(report: LostReport, item: FoundItem, config: MatchingConfig = MatchingConfig.DEFAULT): Int {
        val weighted = config.colorWeight * colorSimilarity(report.color, item.color) +
            config.brandWeight * brandSimilarity(report.brand, item.brand) +
            config.zoneWeight * zoneSimilarity(report.zone, item.foundZone) +
            config.dateWeight * dateSimilarity(report, item) +
            config.textWeight * textSimilarity(report.title + " " + report.description, item.title + " " + item.description)
        // El pequeño margen evita que 99.99999 por redondeo de Double quede en 99.
        return floor(weighted / config.totalWeight * 100 + 1e-9).toInt().coerceIn(0, 100)
    }

    /**
     * Ranking de candidatos para un reporte: pasan el filtro, no fueron descartados ("No es mío"),
     * llegan al umbral y se quedan los [MatchingConfig.topN] mejores. Empates: el encontrado más reciente primero.
     */
    fun rank(
        report: LostReport,
        items: List<FoundItem>,
        config: MatchingConfig = MatchingConfig.DEFAULT,
        excluded: Set<ItemId> = emptySet()
    ): List<Pair<FoundItem, Int>> =
        items.asSequence()
            .filter { it.id !in excluded && isCandidate(report, it, config) }
            .map { it to score(report, it, config) }
            .filter { (_, score) -> score >= config.minScore }
            .sortedWith(compareByDescending<Pair<FoundItem, Int>> { it.second }.thenByDescending { it.first.foundAt })
            .take(config.topN)
            .toList()

    /** Al registrar un hallazgo: qué reportes podrían ser de ese objeto (luego se re-rankea cada uno). */
    fun candidateReports(item: FoundItem, reports: List<LostReport>, config: MatchingConfig = MatchingConfig.DEFAULT) =
        reports.filter { isCandidate(it, item, config) }

    internal fun colorSimilarity(lost: String?, found: String?): Double = when {
        lost.isNullOrBlank() || found.isNullOrBlank() -> UNKNOWN
        lost.normalized() == found.normalized() -> 1.0
        else -> 0.0
    }

    internal fun brandSimilarity(lost: String?, found: String?): Double = when {
        lost.isNullOrBlank() || found.isNullOrBlank() -> UNKNOWN
        // "Casio fx-991" y "Casio" cuentan como la misma marca.
        lost.normalized().contains(found.normalized()) || found.normalized().contains(lost.normalized()) -> 1.0
        else -> 0.0
    }

    internal fun zoneSimilarity(lost: CampusZone, found: CampusZone): Double = when {
        lost == found -> 1.0
        lost == CampusZone.OTRO || found == CampusZone.OTRO -> UNKNOWN
        else -> 0.0
    }

    /** Encontrado dentro del rango: 1; después, baja 0.1 cada 3 días. */
    internal fun dateSimilarity(report: LostReport, item: FoundItem): Double {
        if (item.foundAt <= report.lostTo) return 1.0
        val daysLate = (item.foundAt - report.lostTo).inWholeDays.toInt()
        return (1.0 - (daysLate / DAYS_PER_STEP) * STEP).coerceAtLeast(0.0)
    }

    /**
     * Parecido de las descripciones: palabras en común sobre las del texto más corto (así "Calculadora Casio"
     * contra una descripción larga que la menciona puntúa alto). Sin palabras útiles en algún lado: 0.5.
     */
    internal fun textSimilarity(lost: String, found: String): Double {
        val a = keywords(lost)
        val b = keywords(found)
        if (a.isEmpty() || b.isEmpty()) return UNKNOWN
        return (a intersect b).size.toDouble() / minOf(a.size, b.size)
    }

    internal fun keywords(text: String): Set<String> =
        text.lowercase()
            .map { ACCENTS[it] ?: it }
            .joinToString("")
            .split(Regex("[^a-z0-9ñ]+"))
            .filter { it.length >= MIN_WORD && it !in STOPWORDS }
            .toSet()

    private fun String.normalized() = trim().lowercase()

    private const val UNKNOWN = 0.5
    private const val DAYS_PER_STEP = 3
    private const val STEP = 0.1
    private const val MIN_WORD = 3
    private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')
    private val STOPWORDS = setOf(
        "con", "sin", "del", "los", "las", "una", "uno", "por", "para", "que", "muy", "esta", "este", "tiene", "color"
    )
}
