package bo.edu.ucb.encuentra.feature.dashboard

import bo.edu.ucb.encuentra.core.RecordingMessages
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.presentation.MessageKind
import bo.edu.ucb.encuentra.core.util.Csv
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditTargetKind
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DateRange
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportBuilder
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportData
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportKind
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportRequest
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.ExportRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.BuildExportUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveExportDataUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportEffect
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportViewModel
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.myreports.RecordingAudit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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

private val now = FixedClock.now() // 2026-11-02 12:00 (La Paz)
private val all = DateRange(null, null)

private val data = ExportData(
    items = SeedData.foundItems,
    userNames = SeedData.users.associate { it.id to it.fullName },
    audit = listOf(
        AuditEntry(2, AuditAction.CLAIM_APPROVED, now - 2.hours, "u-ana-staff", "Ana Staff", "c-1024", AuditTargetKind.RECLAMO,
            "Reclamo #1024 · Billetera Negra de Cuero", "fi-billetera", "Pendiente → Aprobado; \"carnet\" verificado"),
        AuditEntry(1, AuditAction.ITEM_DONATED, now - 40.days, "u-ana-staff", "Ana Staff", "fi-paraguas", AuditTargetKind.OBJETO,
            "Paraguas Negro Plegable (#PARAGUAS)", "fi-paraguas", "=SUM(A1)")
    )
)

/** Separa el CSV en filas y celdas (sin comillas: los datos de prueba no las necesitan salvo donde se verifica). */
private fun lines(csv: String) = csv.removePrefix(Csv.BOM).trimEnd().split("\r\n")

@OptIn(ExperimentalCoroutinesApi::class)
class ExportTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun csvCellsAreQuotedAndFormulasNeutralized() {
        assertEquals("Llavero", Csv.cell("Llavero"))
        assertEquals("\"Bloque A; aula 2\"", Csv.cell("Bloque A; aula 2"))
        assertEquals("\"Dijo \"\"es mío\"\"\"", Csv.cell("Dijo \"es mío\""))
        assertEquals("\"Línea 1\nLínea 2\"", Csv.cell("Línea 1\nLínea 2"))
        assertEquals("'=SUM(A1)", Csv.cell("=SUM(A1)"))
        assertEquals("'-5", Csv.cell("-5"))
        val csv = Csv.build(listOf("A", "B"), listOf(listOf("1", "2")))
        assertTrue(csv.startsWith(Csv.BOM))
        assertEquals("A;B\r\n1;2\r\n", csv.removePrefix(Csv.BOM))
    }

    @Test
    fun inventoryExportHasHeaderAndReadableColumns() {
        val file = ExportBuilder.build(data, ExportRequest(ExportKind.INVENTARIO, all), now, BoliviaTime)
        val rows = lines(file.content)
        assertEquals("inventario_2026-11-02.csv", file.name)
        assertEquals(SeedData.foundItems.size, file.rows)
        assertEquals(ExportBuilder.INVENTORY_HEADER.joinToString(";"), rows.first())
        // El más reciente primero: el cuaderno (1 nov), con fecha dd/mm/aaaa y quién lo registró.
        val cuaderno = rows[1].split(";")
        assertEquals("#CUADERNO", cuaderno[0])
        assertEquals("01/11/2026 10:45", cuaderno[8])
        assertEquals("Disponible", cuaderno[9])
        assertEquals("Vigente", cuaderno[10])
        assertEquals("Ana Staff", cuaderno.last())
    }

    @Test
    fun inventoryFiltersByStatusAndDate() {
        val claimed = ExportRequest(ExportKind.INVENTARIO, all, ItemStatus.RECLAMADO)
        assertEquals(3, ExportBuilder.count(data, claimed, BoliviaTime)) // calculadora, billetera, apuntes
        val october = ExportRequest(ExportKind.INVENTARIO, DateRange(kotlinx.datetime.LocalDate(2026, 10, 1), kotlinx.datetime.LocalDate(2026, 10, 31)))
        assertEquals(4, ExportBuilder.count(data, october, BoliviaTime)) // calculadora, termo, billetera, apuntes
    }

    @Test
    fun auditExportKeepsWhoWhatAndWhen() {
        val file = ExportBuilder.build(data, ExportRequest(ExportKind.AUDITORIA, all), now, BoliviaTime)
        val rows = lines(file.content)
        assertEquals("Fecha;Hora;Acción;Persona;Sobre;Detalle", rows[0])
        assertEquals("02/11/2026;10:00;Reclamo aprobado;Ana Staff;Reclamo #1024 · Billetera Negra de Cuero;\"Pendiente → Aprobado; \"\"carnet\"\" verificado\"", rows[1])
        assertTrue(rows[2].endsWith(";'=SUM(A1)"))
        // Solo la última semana: queda la aprobación.
        val week = ExportRequest(ExportKind.AUDITORIA, DateRange(kotlinx.datetime.LocalDate(2026, 10, 27), kotlinx.datetime.LocalDate(2026, 11, 2)))
        assertEquals(1, ExportBuilder.count(data, week, BoliviaTime))
    }

    @Test
    fun exportingIsStaffOnlyAndAudited() = runTest(dispatcher) {
        val session = FakeSession(demoUser(Role.ESTUDIANTE))
        val audit = RecordingAudit()
        val build = BuildExportUseCase(GetCurrentUserUseCase(session), audit, FixedClock, BoliviaTime)
        val request = ExportRequest(ExportKind.AUDITORIA, all)

        assertEquals(BuildExportUseCase.NOT_STAFF, build(data, request).exceptionOrNull()?.message)
        session.setCurrentUser(demoUser(Role.STAFF))
        assertEquals(2, build(data, request).getOrThrow().rows)
        assertEquals(listOf(AuditAction.DATA_EXPORTED), audit.actions)
    }

    @Test
    fun viewModelSharesTheFileWithTheCurrentFilters() = runTest(dispatcher) {
        val session = FakeSession(demoUser(Role.STAFF))
        val user = GetCurrentUserUseCase(session)
        val repository = object : ExportRepository {
            override fun observeData() = flowOf(data)
        }
        val messages = RecordingMessages()
        val vm = ExportViewModel(
            ObserveExportDataUseCase(repository, user),
            BuildExportUseCase(user, RecordingAudit(), FixedClock, BoliviaTime),
            messages, FixedClock, BoliviaTime
        )
        val effect = async { vm.effects.first() }
        advanceUntilIdle()
        assertEquals(5, vm.state.value.rows) // inventario de los últimos 30 días (4 oct - 2 nov): calculadora, termo, apuntes, billetera, cuaderno

        vm.handleEvent(ExportEvent.OnPeriodSelected(MetricsPeriod.TODO))
        vm.handleEvent(ExportEvent.OnStatusSelected(ItemStatus.DISPONIBLE))
        vm.handleEvent(ExportEvent.OnExport)
        advanceUntilIdle()
        val share = effect.await() as ExportEffect.Share
        assertEquals(5, share.file.rows)

        vm.onShared(success = false, file = share.file)
        assertEquals(MessageKind.ERROR, messages.last?.kind)
    }
}
