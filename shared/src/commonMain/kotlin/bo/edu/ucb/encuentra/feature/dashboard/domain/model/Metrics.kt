package bo.edu.ucb.encuentra.feature.dashboard.domain.model

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** Un objeto encontrado y cuándo salió de custodia (entrega, donación o descarte); null si sigue en custodia. */
data class ItemOutcome(val item: FoundItem, val exitAt: Instant?)

/** Rango de fechas del filtro (por fecha en que se encontró el objeto). */
enum class MetricsPeriod(val label: String) {
    ULTIMOS_30("30 días"),
    ULTIMOS_90("90 días"),
    ESTE_ANIO("Este año"),
    TODO("Todo"),
    PERSONALIZADO("Rango")
}

/** Desde y hasta (inclusive); null = sin límite. */
data class DateRange(val from: LocalDate?, val to: LocalDate?) {
    companion object {
        fun of(period: MetricsPeriod, today: LocalDate, custom: DateRange): DateRange = when (period) {
            MetricsPeriod.ULTIMOS_30 -> DateRange(today.minus(DatePeriod(days = 29)), today)
            MetricsPeriod.ULTIMOS_90 -> DateRange(today.minus(DatePeriod(days = 89)), today)
            MetricsPeriod.ESTE_ANIO -> DateRange(LocalDate(today.year, 1, 1), today)
            MetricsPeriod.TODO -> DateRange(null, null)
            MetricsPeriod.PERSONALIZADO -> custom
        }
    }
}

/** Recuperación de una categoría: de los encontrados, cuántos volvieron a su dueño. */
data class CategoryRecovery(val category: Category, val found: Int, val returned: Int) {
    /** 0..1 */
    val rate: Double get() = if (found == 0) 0.0 else returned.toDouble() / found
    val percent: Int get() = (rate * 100).roundToInt()
}

/**
 * D02 · Métricas para la oficina de Objetos Perdidos sobre los objetos encontrados en el rango.
 * - Promedio hasta la entrega: de "encontrado" a "entregado a su dueño".
 * - Promedio de retención: de "encontrado" a que salió de custodia por cualquier motivo (entrega, donación o
 *   descarte); los que siguen en custodia se miden aparte (días que llevan hoy).
 */
data class Metrics(
    val found: Int,
    val returned: Int,
    val donated: Int,
    val discarded: Int,
    val inCustody: Int,
    val avgDaysToReturn: Double?,
    val avgRetentionDays: Double?,
    val avgDaysInCustodyNow: Double?,
    val byCategory: List<CategoryRecovery>
) {
    val recoveryPercent: Int? get() = if (found == 0) null else (returned * 100.0 / found).roundToInt()
    val isEmpty: Boolean get() = found == 0
}

object MetricsCalculator {

    fun compute(outcomes: List<ItemOutcome>, range: DateRange, now: Instant, timeZone: TimeZone): Metrics {
        val start = range.from?.atStartOfDayIn(timeZone)
        val endExclusive = range.to?.plus(DatePeriod(days = 1))?.atStartOfDayIn(timeZone)
        val inRange = outcomes.filter { (item) ->
            (start == null || item.foundAt >= start) && (endExclusive == null || item.foundAt < endExclusive)
        }
        val returned = inRange.filter { it.item.status == ItemStatus.ENTREGADO }
        val left = inRange.filter { it.item.status in EXITS && it.exitAt != null }
        val custody = inRange.filter { it.item.status in IN_CUSTODY }

        return Metrics(
            found = inRange.size,
            returned = returned.size,
            donated = inRange.count { it.item.status == ItemStatus.DONADO },
            discarded = inRange.count { it.item.status == ItemStatus.DESCARTADO },
            inCustody = custody.size,
            avgDaysToReturn = returned.mapNotNull { o -> o.exitAt?.let { days(it - o.item.foundAt) } }.averageOrNull(),
            avgRetentionDays = left.map { days(it.exitAt!! - it.item.foundAt) }.averageOrNull(),
            avgDaysInCustodyNow = custody.map { days(now - it.item.foundAt) }.averageOrNull(),
            byCategory = inRange.groupBy { it.item.category }
                .map { (category, items) -> CategoryRecovery(category, items.size, items.count { it.item.status == ItemStatus.ENTREGADO }) }
                .sortedWith(compareByDescending<CategoryRecovery> { it.rate }.thenByDescending { it.found })
        )
    }

    /** "Hoy" en la zona del teléfono, para armar los rangos. */
    fun today(now: Instant, timeZone: TimeZone): LocalDate = now.toLocalDateTime(timeZone).date

    private fun days(duration: Duration): Double = duration.toDouble(DurationUnit.DAYS).coerceAtLeast(0.0)

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private val EXITS = setOf(ItemStatus.ENTREGADO, ItemStatus.DONADO, ItemStatus.DESCARTADO)
    private val IN_CUSTODY = setOf(ItemStatus.DISPONIBLE, ItemStatus.RECLAMADO)
}
