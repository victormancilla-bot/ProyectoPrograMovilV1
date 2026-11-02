package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.engine.MatchEngine
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore
import kotlin.time.Clock

/**
 * Matching fase 1 (M01): corre el [MatchEngine] al crear un reporte (L04) y al registrar un hallazgo (H03),
 * guarda los Match nuevos como PROPUESTO y avisa al dueño de cada reporte con una notificación NEW_MATCH
 * (es lo que cuenta "N coincidencias nuevas" del Home). Reemplaza al hook vacío de L04.
 */
class RunMatchingUseCase(
    private val store: MatchingStore,
    private val clock: Clock
) : MatchingTrigger {

    override suspend fun onReportCreated(report: LostReport) {
        val pairs = MatchEngine.rank(report, store.availableFoundItems()).map { (item, score) -> report to item to score }
        saveAndNotify(pairs)
    }

    override suspend fun onFoundItemRegistered(item: FoundItem) {
        val pairs = MatchEngine.rank(item, store.searchingReports()).map { (report, score) -> report to item to score }
        saveAndNotify(pairs)
    }

    private suspend fun saveAndNotify(pairs: List<Pair<Pair<LostReport, FoundItem>, Int>>) {
        if (pairs.isEmpty()) return
        val now = clock.now()
        val reports = pairs.associate { (pair, _) -> pair.first.id to pair.first }
        val items = pairs.associate { (pair, _) -> pair.second.id to pair.second }
        val created = store.saveNewMatches(
            pairs.map { (pair, score) ->
                val (report, item) = pair
                Match(
                    id = matchId(report.id, item.id),
                    lostReportId = report.id,
                    foundItemId = item.id,
                    score = score,
                    status = MatchStatus.PROPUESTO,
                    createdAt = now
                )
            }
        )
        // Una notificación por reporte, que abre su mejor coincidencia nueva.
        val notifications = created.groupBy { it.lostReportId }.mapNotNull { (reportId, matches) ->
            val report = reports[reportId] ?: return@mapNotNull null
            val best = matches.maxBy { it.score }
            val bestItem = items[best.foundItemId] ?: return@mapNotNull null
            AppNotification(
                id = ItemId.generate(),
                userId = report.ownerId,
                type = NotificationType.NEW_MATCH,
                title = if (matches.size == 1) "1 coincidencia nueva" else "${matches.size} coincidencias nuevas",
                body = "Encontramos \"${bestItem.title}\" que coincide en ${best.score} % con tu reporte \"${report.title}\".",
                deepLink = "ucbencuentra://match/${best.id.value}",
                read = false,
                createdAt = now
            )
        }
        store.notify(notifications)
    }

    companion object {
        /** Id fijo por par: correr el matching dos veces no duplica la coincidencia. */
        fun matchId(reportId: ItemId, itemId: ItemId) = ItemId("m-${reportId.value}-${itemId.value}")
    }
}
