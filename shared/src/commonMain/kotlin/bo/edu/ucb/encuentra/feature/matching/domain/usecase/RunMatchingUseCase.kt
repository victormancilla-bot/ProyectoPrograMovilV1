package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.engine.MatchEngine
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingConfigRepository
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore
import kotlin.time.Clock

/**
 * Matching (M01-M02): re-calcula el ranking de un reporte con los pesos de Remote Config.
 * Corre al crear o editar un reporte (L04 / L05) y al registrar un hallazgo (H03), para cada reporte afectado.
 *
 * Solo se re-calculan reportes BUSCANDO (uno en verificación o confirmado no se toca). Por reporte:
 * los "No es mío" (DESCARTADO) y los CONFIRMADO no se tocan ni vuelven a proponerse; del Top-N nuevo se
 * crean los pares que no existían (con aviso NEW_MATCH al dueño), se actualiza el score de los PROPUESTO
 * que siguen y se borran los PROPUESTO cuyo objeto sigue disponible pero quedó fuera del Top-N
 * (si el objeto cambió de estado, p. ej. lo están reclamando, de eso se encarga el flujo de reclamos).
 */
class RunMatchingUseCase(
    private val store: MatchingStore,
    private val configRepository: MatchingConfigRepository,
    private val clock: Clock
) : MatchingTrigger {

    override suspend fun onReportCreated(report: LostReport) = rematch(listOf(report))

    override suspend fun onReportUpdated(report: LostReport) = rematch(listOf(report))

    override suspend fun onFoundItemRegistered(item: FoundItem) {
        val config = configRepository.get()
        rematch(MatchEngine.candidateReports(item, store.searchingReports(), config), config)
    }

    private suspend fun rematch(reports: List<LostReport>, config: MatchingConfig? = null) {
        if (reports.isEmpty()) return
        val weights = config ?: configRepository.get()
        val items = store.availableFoundItems()
        val notifications = reports.mapNotNull { report -> rematchReport(report, items, weights) }
        store.notify(notifications)
    }

    /** Devuelve la notificación para el dueño si aparecieron coincidencias nuevas. */
    private suspend fun rematchReport(report: LostReport, items: List<FoundItem>, config: MatchingConfig): AppNotification? {
        if (report.status != ReportStatus.BUSCANDO) return null
        val existing = store.matchesForReport(report.id).associateBy { it.foundItemId }
        val locked = existing.values.filter { it.status != MatchStatus.PROPUESTO }.map { it.foundItemId }.toSet()
        val ranked = MatchEngine.rank(report, items, config, excluded = locked)
        val now = clock.now()

        val created = mutableListOf<Pair<Match, FoundItem>>()
        val toSave = ranked.mapNotNull { (item, score) ->
            val previous = existing[item.id]
            when {
                previous == null -> Match(matchId(report.id, item.id), report.id, item.id, score, MatchStatus.PROPUESTO, now)
                    .also { created += it to item }
                previous.score != score -> previous.copyWithScore(score)
                else -> null
            }
        }
        val rankedIds = ranked.map { it.first.id }.toSet()
        val availableIds = items.map { it.id }.toSet()
        val stale = existing.values
            .filter { it.status == MatchStatus.PROPUESTO && it.foundItemId in availableIds && it.foundItemId !in rankedIds }
            .map { it.id }

        store.saveMatches(toSave)
        store.deleteMatches(stale)
        return created.maxByOrNull { it.first.score }?.let { (best, item) -> notificationFor(report, created.size, best, item) }
    }

    private fun notificationFor(report: LostReport, count: Int, best: Match, item: FoundItem) = AppNotification(
        id = ItemId.generate(),
        userId = report.ownerId,
        type = NotificationType.NEW_MATCH,
        title = if (count == 1) "1 coincidencia nueva" else "$count coincidencias nuevas",
        body = "Encontramos \"${item.title}\" que coincide en ${best.score} % con tu reporte \"${report.title}\".",
        deepLink = "ucbencuentra://match/${best.id.value}",
        read = false,
        createdAt = clock.now()
    )

    private fun Match.copyWithScore(score: Int) = Match(id, lostReportId, foundItemId, score, status, createdAt)

    companion object {
        /** Id fijo por par: correr el matching dos veces no duplica la coincidencia. */
        fun matchId(reportId: ItemId, itemId: ItemId) = ItemId("m-${reportId.value}-${itemId.value}")
    }
}
