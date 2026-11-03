package bo.edu.ucb.encuentra.feature.alerts

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.alerts.domain.repository.AlertsRepository
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ClearReadAlertsUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAlertReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAllAlertsReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveAlertsUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveUnreadAlertsCountUseCase
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsEffect
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsEvent
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsFilter
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsViewModel
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** Tabla `notifications` en memoria. */
internal class InMemoryAlerts(initial: List<AppNotification>) : AlertsRepository {
    val all = MutableStateFlow(initial)

    override fun observe(userId: String): Flow<List<AppNotification>> =
        all.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun observeUnreadCount(userId: String): Flow<Int> = observe(userId).map { list -> list.count { !it.read } }

    override suspend fun markRead(id: String) = all.update { list -> list.map { if (it.id.value == id) it.copy(read = true) else it } }

    override suspend fun markAllRead(userId: String) =
        all.update { list -> list.map { if (it.userId == userId) it.copy(read = true) else it } }

    override suspend fun clearRead(userId: String) = all.update { list -> list.filterNot { it.userId == userId && it.read } }
}

private fun alert(id: String, userId: String = "u-mateo", read: Boolean = false, minutesAgo: Int = 0, link: String? = "ucbencuentra://match/m-$id") =
    AppNotification(
        id = ItemId(id),
        userId = userId,
        type = NotificationType.NEW_MATCH,
        title = "Alerta $id",
        body = "Cuerpo",
        deepLink = link,
        read = read,
        createdAt = FixedClock.now() - minutesAgo.minutes
    )

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsTest {

    private val dispatcher = StandardTestDispatcher()
    private val session = FakeSession(mateo)
    private val getCurrentUser = GetCurrentUserUseCase(session)
    private val repository = InMemoryAlerts(
        listOf(
            alert("nueva", minutesAgo = 5),
            alert("vieja", read = true, minutesAgo = 60),
            alert("sin-link", minutesAgo = 10, link = null),
            alert("de-ana", userId = "u-ana-gomez")
        )
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AlertsViewModel(
        ObserveAlertsUseCase(repository, getCurrentUser),
        MarkAlertReadUseCase(repository, getCurrentUser),
        MarkAllAlertsReadUseCase(repository, getCurrentUser),
        ClearReadAlertsUseCase(repository, getCurrentUser),
        FixedClock
    )

    private fun readOf(id: String) = repository.all.value.first { it.id.value == id }.read

    @Test
    fun listShowsOnlyMyAlertsUnreadFirst() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(listOf("nueva", "sin-link"), state.unread.map { it.id.value })
        assertEquals(listOf("vieja"), state.earlier.map { it.id.value })
        assertEquals(2, state.unreadCount)

        vm.handleEvent(AlertsEvent.OnFilterChanged(AlertsFilter.NO_LEIDAS))
        assertTrue(vm.state.value.earlier.isEmpty())
    }

    @Test
    fun openingAnAlertMarksItReadAndOpensItsLink() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<AlertsEffect>()
        val job = launch { vm.effects.collect { effects += it } }

        vm.handleEvent(AlertsEvent.OnOpen(vm.state.value.unread.first()))
        advanceUntilIdle()

        assertTrue(readOf("nueva"))
        assertEquals(listOf<AlertsEffect>(AlertsEffect.OpenLink("ucbencuentra://match/m-nueva")), effects)
        assertEquals(1, vm.state.value.unreadCount)

        // Sin deep link: solo se marca como leída.
        vm.handleEvent(AlertsEvent.OnOpen(vm.state.value.unread.first()))
        advanceUntilIdle()
        assertTrue(readOf("sin-link"))
        assertEquals(1, effects.size)
        job.cancel()
    }

    @Test
    fun markAllAndClearOnlyTouchMyAlerts() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(AlertsEvent.OnMarkAllRead)
        advanceUntilIdle()
        assertEquals(0, vm.state.value.unreadCount)
        assertEquals(false, readOf("de-ana"))

        vm.handleEvent(AlertsEvent.OnRequestClear)
        assertTrue(vm.state.value.confirmClear)
        vm.handleEvent(AlertsEvent.OnConfirmClear)
        advanceUntilIdle()
        assertTrue(vm.state.value.alerts.isEmpty())
        assertEquals(listOf("de-ana"), repository.all.value.map { it.id.value })
    }

    @Test
    fun clearKeepsUnreadAlerts() = runTest(dispatcher) {
        ClearReadAlertsUseCase(repository, getCurrentUser)()
        assertEquals(setOf("nueva", "sin-link", "de-ana"), repository.all.value.map { it.id.value }.toSet())
    }

    @Test
    fun cannotMarkSomeoneElsesAlertAndBadgeCountsUnread() = runTest(dispatcher) {
        MarkAlertReadUseCase(repository, getCurrentUser)(repository.all.value.first { it.id.value == "de-ana" })
        assertEquals(false, readOf("de-ana"))

        val badge = ObserveUnreadAlertsCountUseCase(repository)
        assertEquals(2, badge("u-mateo").first())
        assertEquals(0, badge(null).first())
    }
}
