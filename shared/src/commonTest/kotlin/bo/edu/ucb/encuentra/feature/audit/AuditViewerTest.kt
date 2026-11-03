package bo.edu.ucb.encuentra.feature.audit

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.audit.data.AuditLabels
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditFilter
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditPeriod
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditTargetKind
import bo.edu.ucb.encuentra.feature.audit.domain.repository.AuditTrailRepository
import bo.edu.ucb.encuentra.feature.audit.domain.usecase.ObserveAuditTrailUseCase
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditEvent
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditViewModel
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Nombres del seed (usuarios, objetos, reclamos y reportes). */
private val labels = AuditLabels(
    users = SeedData.users.associate { it.id to it.fullName },
    items = SeedData.foundItems.associateBy { it.id.value },
    claims = SeedData.claims.associateBy { it.id.value },
    reports = SeedData.lostReports.associateBy { it.id.value }
)

private val now = FixedClock.now()

/** Historia del llavero y de otras cosas, en distintas fechas. */
private val logs = listOf(
    AuditLog(1, AuditAction.ITEM_REGISTERED, "u-ana-staff", "fi-llavero", "Llavero con 3 llaves", now - 60.days),
    AuditLog(2, AuditAction.CLAIM_CREATED, "u-mateo", "c-1024", "Reclamo #1024", now - 3.days),
    AuditLog(3, AuditAction.CLAIM_APPROVED, "u-ana-staff", "c-1024", "Pendiente → Aprobado", now - 2.hours),
    AuditLog(4, AuditAction.ROLE_CHANGED, "u-admin", "u-ana-gomez", "Estudiante → Staff", now - 1.hours),
    AuditLog(5, AuditAction.REPORT_CLOSED, "u-mateo", "lr-laptop", "Lo encontré", now - 10.days),
    AuditLog(6, AuditAction.ITEM_DONATED, "u-borrado", "fi-no-existe", "", now - 40.days)
)

private val entries = logs.map(labels::entryFor)

private fun ids(list: List<AuditEntry>) = list.map { it.id }.toSet()

@OptIn(ExperimentalCoroutinesApi::class)
class AuditViewerTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun idsBecomeReadableNamesAndClaimsPointToTheirItem() {
        val claim = entries.first { it.id == 2L }
        assertEquals(AuditTargetKind.RECLAMO, claim.targetKind)
        assertEquals("Reclamo #1024 · Billetera Negra de Cuero", claim.targetLabel)
        assertEquals("Mateo Quispe", claim.actorName)
        assertEquals("Llavero con 3 llaves (#LLAVERO)", entries.first { it.id == 1L }.targetLabel)
        assertEquals("Ana Gómez", entries.first { it.id == 4L }.targetLabel)
        assertEquals("Reporte: Laptop Dell XPS", entries.first { it.id == 5L }.targetLabel)
        // Si algo ya no existe, se muestra el id: la auditoría nunca se pierde.
        assertEquals("fi-no-existe", entries.first { it.id == 6L }.targetLabel)
        assertEquals("u-borrado", entries.first { it.id == 6L }.actorName)
    }

    @Test
    fun filterByObjectFindsTheItemAndItsClaims() {
        // El reclamo #1024 es de la billetera: buscar por su código trae el reclamo y su aprobación.
        assertEquals(setOf(2L, 3L), ids(AuditFilter(query = "#billeter").apply(entries, now, BoliviaTime)))
        assertEquals(setOf(1L), ids(AuditFilter(query = "llavero").apply(entries, now, BoliviaTime)))
        assertEquals(setOf(2L, 3L), ids(AuditFilter(query = "1024").apply(entries, now, BoliviaTime)))
        assertEquals(setOf(4L), ids(AuditFilter(query = "ana gomez").apply(entries, now, BoliviaTime)))
    }

    @Test
    fun filterByPersonAndDate() {
        assertEquals(setOf(2L, 5L), ids(AuditFilter(actorId = "u-mateo").apply(entries, now, BoliviaTime)))
        assertEquals(setOf(3L, 4L), ids(AuditFilter(period = AuditPeriod.HOY).apply(entries, now, BoliviaTime)))
        assertEquals(setOf(2L, 3L, 4L), ids(AuditFilter(period = AuditPeriod.SEMANA).apply(entries, now, BoliviaTime)))
        assertEquals(setOf(2L, 3L, 4L, 5L), ids(AuditFilter(period = AuditPeriod.MES).apply(entries, now, BoliviaTime)))
        assertEquals(setOf(5L), ids(AuditFilter(actorId = "u-mateo", period = AuditPeriod.MES, query = "laptop").apply(entries, now, BoliviaTime)))
    }

    private val repository = object : AuditTrailRepository {
        override fun observeEntries() = flowOf(entries)
    }

    @Test
    fun onlyStaffSeesTheTrailNewestFirst() = runTest(dispatcher) {
        val session = FakeSession(demoUser(Role.STAFF))
        val observe = ObserveAuditTrailUseCase(repository, GetCurrentUserUseCase(session))
        assertEquals(listOf(4L, 3L, 2L, 5L, 6L, 1L), observe().first().map { it.id })

        session.setCurrentUser(demoUser(Role.ESTUDIANTE))
        assertTrue(observe().first().isEmpty())
    }

    @Test
    fun viewModelOffersOnlyPeopleWhoActedAndClearsFilters() = runTest(dispatcher) {
        val vm = AuditViewModel(
            ObserveAuditTrailUseCase(repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.ADMIN)))),
            FixedClock,
            BoliviaTime
        )
        advanceUntilIdle()
        assertEquals(listOf("Admin UCB", "Ana Staff", "Mateo Quispe", "u-borrado"), vm.state.value.people.map { it.name })

        vm.handleEvent(AuditEvent.OnPersonSelected("u-ana-staff"))
        vm.handleEvent(AuditEvent.OnPeriodSelected(AuditPeriod.HOY))
        assertEquals(setOf(3L), ids(vm.state.value.visible))

        vm.handleEvent(AuditEvent.OnClearFilters)
        assertEquals(6, vm.state.value.visible.size)
    }
}
