package bo.edu.ucb.encuentra.feature.dashboard

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.core.presentation.formatRelative
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.StatsSource
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.DashboardRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveDashboardStatsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveLatestPendingClaimsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardEffect
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardViewModel
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.StatTarget
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Lo que ve el seed 2026 en Room: 6 en custodia, 0 por entregar, 1 en verificación, 1 por vencer (el llavero). */
private val LOCAL = DashboardStats(inCustody = 6, pendingDelivery = 0, inVerification = 1, expiringSoon = 1)
private val REMOTE = DashboardStats(inCustody = 142, pendingDelivery = 18, inVerification = 5, expiringSoon = 3)

private class FakeDashboardRepository(
    var realtime: Flow<DashboardStats?> = flow { error("Default FirebaseApp is not initialized") }
) : DashboardRepository {
    val local = MutableStateFlow(LOCAL)
    val expiringQueries = mutableListOf<Instant>()
    val claims = MutableStateFlow(
        listOf(
            PendingClaim(
                ItemId("c-1027"), 1027, "Llavero con 3 llaves", Category.LLAVES, "Mateo Quispe", ClaimStatus.PENDIENTE,
                FixedClock.now() - 2.hours
            )
        )
    )

    override fun observeRealtimeStats() = realtime
    override fun observeLocalStats(expiringBefore: Instant): Flow<DashboardStats> {
        expiringQueries += expiringBefore
        return local
    }
    override fun observeLatestPendingClaims(limit: Int) = claims
}

class ObserveDashboardStatsUseCaseTest {

    @Test
    fun withoutFirebaseTheCountersComeFromRoom() = runTest {
        val repository = FakeDashboardRepository()
        val live = ObserveDashboardStatsUseCase(repository, flowOf(FixedClock.now()))().first()

        assertEquals(LOCAL, live.stats)
        assertEquals(StatsSource.LOCAL, live.source)
        // "Por vencer" = disponibles que vencen en los próximos 7 días.
        assertEquals(FixedClock.now() + 7.days, repository.expiringQueries.single())
    }

    @Test
    fun realtimeDatabaseReplacesTheLocalCountersWhenItArrives() = runTest {
        // Firebase responde después de un rato (red).
        val repository = FakeDashboardRepository(realtime = flow { delay(500); emit(REMOTE) })

        val emissions = ObserveDashboardStatsUseCase(repository, flowOf(FixedClock.now()))().take(2).toList()

        // Primero lo local (sin esperar a la red), después lo de Firebase.
        assertEquals(listOf(StatsSource.LOCAL, StatsSource.REALTIME_DATABASE), emissions.map { it.source })
        assertEquals(REMOTE, emissions.last().stats)
    }

    @Test
    fun aMissingStatsNodeKeepsTheLocalCounters() = runTest {
        val repository = FakeDashboardRepository(realtime = flowOf(null))
        val live = ObserveDashboardStatsUseCase(repository, flowOf(FixedClock.now()))().first()
        assertEquals(StatsSource.LOCAL, live.source)
    }

    @Test
    fun localCountersUpdateWithoutRefreshing() = runTest {
        val repository = FakeDashboardRepository()
        val observe = ObserveDashboardStatsUseCase(repository, flowOf(FixedClock.now()))()

        val emissions = async { observe.take(2).toList() }
        testScheduler.runCurrent()
        repository.local.value = LOCAL.copy(inVerification = 2)

        assertEquals(listOf(1, 2), emissions.await().map { it.stats.inVerification })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeDashboardRepository()

    private fun viewModel() = DashboardViewModel(
        ObserveDashboardStatsUseCase(repository, flowOf(FixedClock.now())),
        ObserveLatestPendingClaimsUseCase(repository),
        GetCurrentUserUseCase(FakeSession(demoUser(Role.STAFF))),
        FixedClock
    )

    @Test
    fun showsCampusCountersAndLatestClaims() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("La Paz", state.campus)
        assertEquals(LOCAL, state.stats)
        assertEquals(listOf(1027), state.latestClaims.map { it.number })
        assertNull(state.error)
    }

    @Test
    fun aNewClaimAppearsWithoutRefreshing() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        repository.claims.value = repository.claims.value + repository.claims.value.first().copy(claimId = ItemId("c-1028"), number = 1028)
        repository.local.value = LOCAL.copy(inVerification = 2)
        advanceUntilIdle()

        assertEquals(listOf(1027, 1028), vm.state.value.latestClaims.map { it.number })
        assertEquals(2, vm.state.value.stats?.inVerification)
    }

    @Test
    fun actionsNavigate() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        suspend fun effectOf(event: DashboardEvent): DashboardEffect {
            val effect = async { vm.effects.first() }
            vm.handleEvent(event)
            advanceUntilIdle()
            return effect.await()
        }

        assertEquals(DashboardEffect.NavigateToFoundForm, effectOf(DashboardEvent.OnRegisterFound))
        assertEquals(DashboardEffect.NavigateToClaims, effectOf(DashboardEvent.OnSeeAllClaims))
        assertEquals(DashboardEffect.NavigateToClaimDetail("c-1027"), effectOf(DashboardEvent.OnClaimClicked("c-1027")))
        assertEquals(DashboardEffect.NavigateToInventory, effectOf(DashboardEvent.OnStatClicked(StatTarget.INVENTORY)))
        assertEquals(DashboardEffect.NavigateToRetention, effectOf(DashboardEvent.OnStatClicked(StatTarget.RETENTION)))
        assertEquals(DashboardEffect.NavigateToAudit, effectOf(DashboardEvent.OnOpenAudit))
        assertEquals(DashboardEffect.NavigateToMetrics, effectOf(DashboardEvent.OnOpenMetrics))
    }
}

class FormatRelativeTest {
    private val now = FixedClock.now()

    @Test
    fun relativeTimes() {
        assertEquals("Hace un momento", formatRelative(now - 20.minutes / 60, now))
        assertEquals("Hace 5 min", formatRelative(now - 5.minutes, now))
        assertEquals("Hace 1 hora", formatRelative(now - 1.hours, now))
        assertEquals("Hace 2 horas", formatRelative(now - 2.hours, now))
        assertEquals("Hace 3 días", formatRelative(now - 3.days, now))
        assertEquals("19 oct 2026", formatRelative(now - 14.days, now))
    }
}
