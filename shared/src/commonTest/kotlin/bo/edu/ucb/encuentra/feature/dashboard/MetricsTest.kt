package bo.edu.ucb.encuentra.feature.dashboard

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.dashboard.data.outcomesOf
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DateRange
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ItemOutcome
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsCalculator
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.MetricsRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveMetricsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.screen.days
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsViewModel
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.matching.foundItem
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

private val now = FixedClock.now() // 2026-11-02 12:00 (La Paz)

/** Objeto encontrado hace [foundDaysAgo] días que salió [exitDaysAgo] días atrás (null = sigue en custodia). */
private fun outcome(id: String, sub: Subcategory, status: ItemStatus, foundDaysAgo: Int, exitDaysAgo: Int? = null) =
    ItemOutcome(foundItem(id = id, subcategory = sub, status = status, foundAt = now - foundDaysAgo.days), exitDaysAgo?.let { now - it.days })

private val sample = listOf(
    outcome("fi-calc", Subcategory.CALCULADORA, ItemStatus.ENTREGADO, foundDaysAgo = 10, exitDaysAgo = 6), // 4 días
    outcome("fi-laptop", Subcategory.LAPTOP, ItemStatus.ENTREGADO, foundDaysAgo = 20, exitDaysAgo = 18), // 2 días
    outcome("fi-cel", Subcategory.CELULAR, ItemStatus.DISPONIBLE, foundDaysAgo = 5), // en custodia 5 días
    outcome("fi-termo", Subcategory.TERMO, ItemStatus.DONADO, foundDaysAgo = 100, exitDaysAgo = 9), // 91 días
    outcome("fi-gorra", Subcategory.GORRA, ItemStatus.RECLAMADO, foundDaysAgo = 1), // en custodia 1 día
    outcome("fi-llave", Subcategory.LLAVERO, ItemStatus.DESCARTADO, foundDaysAgo = 200, exitDaysAgo = 105) // 95 días
)

private val all = DateRange(null, null)

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun averageRetentionAndTimeToReturn() {
        val m = MetricsCalculator.compute(sample, all, now, BoliviaTime)
        assertEquals(6, m.found)
        assertEquals(2, m.returned)
        assertEquals(1, m.donated)
        assertEquals(1, m.discarded)
        assertEquals(2, m.inCustody)
        assertEquals(3.0, m.avgDaysToReturn) // (4 + 2) / 2
        assertEquals(48.0, m.avgRetentionDays) // (4 + 2 + 91 + 95) / 4
        assertEquals(3.0, m.avgDaysInCustodyNow) // (5 + 1) / 2
        assertEquals(33, m.recoveryPercent)
    }

    @Test
    fun recoveryRateByCategoryBestFirst() {
        val rows = MetricsCalculator.compute(sample, all, now, BoliviaTime).byCategory
        assertEquals(listOf(Category.ELECTRONICA, Category.ROPA_ACCESORIOS, Category.LLAVES), rows.map { it.category })
        assertEquals(67, rows.first().percent) // 2 de 3 devueltos
        assertEquals(listOf(3 to 2, 2 to 0, 1 to 0), rows.map { it.found to it.returned })
    }

    @Test
    fun dateRangeFiltersByFoundDate() {
        val today = MetricsCalculator.today(now, BoliviaTime)
        val last30 = DateRange.of(MetricsPeriod.ULTIMOS_30, today, all)
        assertEquals(LocalDate(2026, 10, 4), last30.from)
        val m = MetricsCalculator.compute(sample, last30, now, BoliviaTime)
        assertEquals(4, m.found) // calc, laptop, cel, gorra
        assertEquals(50, m.recoveryPercent)
        assertNull(MetricsCalculator.compute(sample, DateRange(LocalDate(2025, 1, 1), LocalDate(2025, 1, 31)), now, BoliviaTime).recoveryPercent)
        assertEquals(LocalDate(2026, 1, 1), DateRange.of(MetricsPeriod.ESTE_ANIO, today, all).from)
    }

    @Test
    fun exitDatesComeFromTheDeliveredClaimOrTheAudit() {
        val delivered = foundItem(id = "fi-billetera", status = ItemStatus.ENTREGADO, foundAt = now - 5.days)
        val donated = foundItem(id = "fi-paraguas", status = ItemStatus.DONADO, foundAt = now - 100.days)
        val claim = SeedData.claims.first { it.id.value == "c-1024" }.copy(deliveredAt = now - 1.days)
        val logs = listOf(AuditLog(1, AuditAction.ITEM_DONATED, "u-ana-staff", "fi-paraguas", "", now - 2.days))

        val outcomes = outcomesOf(listOf(delivered, donated), listOf(claim), logs)

        assertEquals(now - 1.days, outcomes[0].exitAt)
        assertEquals(now - 2.days, outcomes[1].exitAt)
    }

    @Test
    fun daysAreReadable() {
        assertEquals("—", days(null))
        assertEquals("1 día", days(1.0))
        assertEquals("3,5 días", days(3.46))
        assertEquals("48 días", days(48.2))
    }

    private val repository = object : MetricsRepository {
        override fun observeOutcomes() = flowOf(sample)
    }

    @Test
    fun onlyStaffSeesMetricsAndTheCustomRangeIsOrdered() = runTest(dispatcher) {
        val session = FakeSession(demoUser(Role.ESTUDIANTE))
        assertTrue(ObserveMetricsUseCase(repository, GetCurrentUserUseCase(session))().first().isEmpty())

        session.setCurrentUser(demoUser(Role.STAFF))
        val vm = MetricsViewModel(ObserveMetricsUseCase(repository, GetCurrentUserUseCase(session)), FixedClock, BoliviaTime)
        advanceUntilIdle()
        assertEquals(6, vm.state.value.metrics.found)

        vm.handleEvent(MetricsEvent.OnPeriodSelected(MetricsPeriod.PERSONALIZADO))
        assertEquals(LocalDate(2026, 10, 4), vm.state.value.custom.from)
        // "Desde" después de "hasta": se intercambian.
        vm.handleEvent(MetricsEvent.OnFromPicked(LocalDate(2026, 11, 1)))
        vm.handleEvent(MetricsEvent.OnToPicked(LocalDate(2026, 10, 20)))
        assertEquals(DateRange(LocalDate(2026, 10, 20), LocalDate(2026, 11, 1)), vm.state.value.custom)
        assertEquals(3, vm.state.value.metrics.found) // calc (23 oct), cel (28 oct) y gorra (1 nov, el "hasta" es inclusivo)
    }
}
