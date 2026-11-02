package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RunMatchingUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeMatchingStore(
    val items: List<FoundItem> = emptyList(),
    val reports: List<LostReport> = emptyList()
) : MatchingStore {
    val matches = mutableListOf<Match>()
    val notifications = mutableListOf<AppNotification>()

    override suspend fun availableFoundItems() = items
    override suspend fun searchingReports() = reports
    override suspend fun saveNewMatches(matches: List<Match>): List<Match> {
        val new = matches.filter { m -> this.matches.none { it.lostReportId == m.lostReportId && it.foundItemId == m.foundItemId } }
        this.matches += new
        return new
    }
    override suspend fun notify(notifications: List<AppNotification>) {
        this.notifications += notifications
    }
}

class RunMatchingUseCaseTest {

    @Test
    fun newReportIsMatchedAgainstAvailableItemsAndTheOwnerIsNotified() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(id = "fi-igual"), foundItem(id = "fi-rojo", color = "Rojo")))
        RunMatchingUseCase(store, FixedClock).onReportCreated(lostReport(id = "lr-nuevo"))

        assertEquals(listOf("fi-igual" to 100, "fi-rojo" to 60), store.matches.map { it.foundItemId.value to it.score })
        assertTrue(store.matches.all { it.status == MatchStatus.PROPUESTO })

        val notification = store.notifications.single()
        assertEquals(NotificationType.NEW_MATCH, notification.type)
        assertEquals("u-mateo", notification.userId)
        assertEquals("2 coincidencias nuevas", notification.title)
        assertEquals("ucbencuentra://match/m-lr-nuevo-fi-igual", notification.deepLink)
    }

    @Test
    fun newFoundItemIsMatchedAgainstSearchingReports() = runTest {
        val store = FakeMatchingStore(reports = listOf(lostReport(id = "lr-a"), lostReport(id = "lr-b", brand = "HP")))
        RunMatchingUseCase(store, FixedClock).onFoundItemRegistered(foundItem(id = "fi-nuevo"))

        assertEquals(listOf("lr-a" to 100, "lr-b" to 75), store.matches.map { it.lostReportId.value to it.score })
        // Un aviso por reporte afectado.
        assertEquals(2, store.notifications.size)
        assertTrue(store.notifications.all { it.title == "1 coincidencia nueva" })
    }

    @Test
    fun runningTwiceDoesNotDuplicateMatchesNorNotifications() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem()))
        val matching = RunMatchingUseCase(store, FixedClock)
        matching.onReportCreated(lostReport())
        matching.onReportCreated(lostReport())
        assertEquals(1, store.matches.size)
        assertEquals(1, store.notifications.size)
    }

    @Test
    fun noCandidatesMeansNoMatchesAndNoNotification() = runTest {
        val store = FakeMatchingStore(items = listOf(foundItem(subcategory = Subcategory.LAPTOP)))
        RunMatchingUseCase(store, FixedClock).onReportCreated(lostReport())
        assertTrue(store.matches.isEmpty())
        assertTrue(store.notifications.isEmpty())
    }
}
