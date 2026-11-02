package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingConfigRepository
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RunMatchingUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class FakeMatchingStore(
    var items: List<FoundItem> = emptyList(),
    var reports: List<LostReport> = emptyList()
) : MatchingStore {
    val matches = mutableMapOf<ItemId, Match>()
    val notifications = mutableListOf<AppNotification>()

    override suspend fun availableFoundItems() = items
    override suspend fun searchingReports() = reports
    override suspend fun matchesForReport(reportId: ItemId) = matches.values.filter { it.lostReportId == reportId }
    override suspend fun saveMatches(matches: List<Match>) = matches.forEach { this.matches[it.id] = it }
    override suspend fun deleteMatches(ids: List<ItemId>) = ids.forEach { matches.remove(it) }
    override suspend fun notify(notifications: List<AppNotification>) {
        this.notifications += notifications
    }

    fun scores() = matches.values.sortedByDescending { it.score }.map { it.foundItemId.value to it.score }
}

internal class FixedConfig(var config: MatchingConfig = PHASE1) : MatchingConfigRepository {
    override suspend fun get() = config
}

class RunMatchingUseCaseTest {

    @Test
    fun newReportIsMatchedAgainstAvailableItemsAndTheOwnerIsNotified() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-igual"), foundItem(id = "fi-rojo", color = "Rojo")))
        RunMatchingUseCase(store, FixedConfig(), FixedClock).onReportCreated(lostReport(id = "lr-nuevo"))

        assertEquals(listOf("fi-igual" to 100, "fi-rojo" to 60), store.scores())
        assertTrue(store.matches.values.all { it.status == MatchStatus.PROPUESTO })

        val notification = store.notifications.single()
        assertEquals(NotificationType.NEW_MATCH, notification.type)
        assertEquals("u-mateo", notification.userId)
        assertEquals("2 coincidencias nuevas", notification.title)
        assertEquals("ucbencuentra://match/m-lr-nuevo-fi-igual", notification.deepLink)
    }

    @Test
    fun newFoundItemReRanksEveryAffectedReport() = runTest {
        val store = FakeMatchingStore(reports = listOf(lostReport(id = "lr-a"), lostReport(id = "lr-b", brand = "HP")))
        store.items = listOf(foundItem(id = "fi-nuevo"))
        RunMatchingUseCase(store, FixedConfig(), FixedClock).onFoundItemRegistered(foundItem(id = "fi-nuevo"))

        assertEquals(setOf("lr-a" to 100, "lr-b" to 75), store.matches.values.map { it.lostReportId.value to it.score }.toSet())
        // Un aviso por reporte afectado.
        assertEquals(2, store.notifications.size)
    }

    @Test
    fun runningTwiceDoesNotDuplicateMatchesNorNotifications() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem()))
        val matching = RunMatchingUseCase(store, FixedConfig(), FixedClock)
        matching.onReportCreated(lostReport())
        matching.onReportUpdated(lostReport())
        assertEquals(1, store.matches.size)
        assertEquals(1, store.notifications.size)
    }

    @Test
    fun rejectedMatchIsNeverProposedAgain() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-1")))
        val matching = RunMatchingUseCase(store, FixedConfig(), FixedClock)
        matching.onReportCreated(lostReport())
        val match = store.matches.values.single()
        store.matches[match.id] = Match(match.id, match.lostReportId, match.foundItemId, match.score, MatchStatus.DESCARTADO, match.createdAt)

        matching.onReportUpdated(lostReport())
        assertEquals(MatchStatus.DESCARTADO, store.matches.values.single().status)
        assertEquals(1, store.notifications.size)
    }

    @Test
    fun editedReportDropsStaleProposalsAndKeepsConfirmedOnes() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-calc"), foundItem(id = "fi-laptop", subcategory = Subcategory.LAPTOP)))
        val matching = RunMatchingUseCase(store, FixedConfig(), FixedClock)
        matching.onReportCreated(lostReport())
        assertEquals(listOf("fi-calc"), store.matches.values.map { it.foundItemId.value })

        // El dueño corrige: no era una calculadora sino una laptop.
        matching.onReportUpdated(lostReport(subcategory = Subcategory.LAPTOP))
        assertEquals(listOf("fi-laptop"), store.matches.values.map { it.foundItemId.value })
    }

    @Test
    fun topNAndNewWeightsComeFromTheConfig() = runTest {
        val items = (1..6).map { foundItem(id = "fi-$it") }
        val store = FakeMatchingStore(items = items)
        val config = FixedConfig(PHASE1.copy(topN = 2))
        val matching = RunMatchingUseCase(store, config, FixedClock)
        matching.onReportCreated(lostReport())
        assertEquals(2, store.matches.size)

        // Un cambio de pesos en Remote Config actualiza el score de lo ya propuesto (mismo Top-2, sin duplicar).
        store.items = (1..6).map { foundItem(id = "fi-$it", color = "Rojo") }
        config.config = PHASE1.copy(topN = 2, colorWeight = 10.0)
        matching.onReportUpdated(lostReport())
        assertEquals(listOf("fi-1" to 85, "fi-2" to 85), store.scores())
    }

    @Test
    fun reportsNotSearchingAreLeftUntouched() = runTest {
        // Regresión: "No es mío" en un reporte EN_VERIFICACION borraba las demás propuestas del seed.
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-1")))
        val matching = RunMatchingUseCase(store, FixedConfig(), FixedClock)
        matching.onReportCreated(lostReport())
        matching.onReportUpdated(lostReport(status = ReportStatus.EN_VERIFICACION, subcategory = Subcategory.LAPTOP))
        assertEquals(listOf("fi-1"), store.matches.values.map { it.foundItemId.value })
    }

    @Test
    fun proposalsForItemsNoLongerAvailableAreKept() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-1"), foundItem(id = "fi-2", color = "Gris")))
        val matching = RunMatchingUseCase(store, FixedConfig(), FixedClock)
        matching.onReportCreated(lostReport())
        // fi-1 pasa a RECLAMADO (ya no está entre los disponibles): su propuesta no se borra.
        store.items = listOf(foundItem(id = "fi-2", color = "Gris"))
        matching.onReportUpdated(lostReport())
        assertEquals(setOf("fi-1", "fi-2"), store.matches.values.map { it.foundItemId.value }.toSet())
    }

    @Test
    fun noCandidatesMeansNoMatchesAndNoNotification() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(subcategory = Subcategory.LAPTOP)))
        RunMatchingUseCase(store, FixedConfig(), FixedClock).onReportCreated(lostReport())
        assertTrue(store.matches.isEmpty())
        assertTrue(store.notifications.isEmpty())
    }
}
