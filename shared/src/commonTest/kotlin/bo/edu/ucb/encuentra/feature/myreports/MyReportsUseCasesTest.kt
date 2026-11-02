package bo.edu.ucb.encuentra.feature.myreports

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportFilter
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportMatchSummary
import bo.edu.ucb.encuentra.feature.myreports.domain.repository.MyReportsRepository
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.CloseReportUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveMyReportsUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveReportDetailUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reportes y coincidencias del seed 2026 en memoria (lo mismo que ve Mateo en la app). */
internal class FakeMyReportsRepository : MyReportsRepository {
    val reports = MutableStateFlow(SeedData.lostReports)

    override fun observeByOwner(ownerId: String): Flow<List<LostReport>> =
        reports.map { all -> all.filter { it.ownerId == ownerId } }

    override fun observeReport(reportId: ItemId): Flow<LostReport?> =
        reports.map { all -> all.firstOrNull { it.id == reportId } }

    override fun observeMatches(reportId: ItemId): Flow<List<ReportMatchSummary>> = MutableStateFlow(
        SeedData.matches.filter { it.lostReportId == reportId }.map { match ->
            val item = SeedData.foundItems.first { it.id == match.foundItemId }
            ReportMatchSummary(match.id, item.title, item.category, item.foundAt, match.score, match.status)
        }
    )

    override suspend fun getReport(reportId: ItemId) = reports.value.firstOrNull { it.id == reportId }

    override suspend fun updateStatus(reportId: ItemId, status: ReportStatus) {
        reports.value = reports.value.map { if (it.id == reportId) it.copy(status = status) else it }
    }
}

internal class RecordingAudit : AuditRecorder {
    val actions = mutableListOf<AuditAction>()
    override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
        actions += action
    }
}

internal val mateo = demoUser(Role.ESTUDIANTE).copy(id = "u-mateo")

class MyReportsUseCasesTest {

    private val repository = FakeMyReportsRepository()
    private val session = GetCurrentUserUseCase(FakeSession(mateo))

    @Test
    fun activeReportsComeFirstNewestFirst() = runTest {
        val titles = ObserveMyReportsUseCase(repository, session)().first().map { it.title }
        // La Botella (VENCIDO) va al final aunque sea de las más viejas igual.
        assertEquals("Botella de Agua Azul", titles.last())
        assertEquals("Laptop Dell XPS", titles.first())
        assertEquals(5, titles.size)
    }

    @Test
    fun filtersMatchTheMockupChips() = runTest {
        val reports = ObserveMyReportsUseCase(repository, session)().first()
        assertEquals(listOf("Laptop Dell XPS"), reports.filter { ReportFilter.BUSCANDO.accepts(it.status) }.map { it.title })
        assertEquals(
            setOf("Lentes Ray-Ban", "Cuaderno de Cálculo", "Calculadora científica"),
            reports.filter { ReportFilter.VERIFICACION.accepts(it.status) }.map { it.title }.toSet()
        )
        assertEquals(5, reports.count { ReportFilter.TODOS.accepts(it.status) })
    }

    @Test
    fun detailHasItsMatchesBestFirst() = runTest {
        val detail = ObserveReportDetailUseCase(repository)(ItemId("lr-calculadora")).first()
        assertEquals(listOf(92, 65, 30), detail?.matches?.map { it.score })
        assertNull(ObserveReportDetailUseCase(repository)(ItemId("no-existe")).first())
    }

    @Test
    fun closingASearchingReportChangesItsStatusAndIsAudited() = runTest {
        val audit = RecordingAudit()
        val close = CloseReportUseCase(repository, session, audit)
        assertTrue(close(ItemId("lr-laptop"), CloseReason.FOUND_IT).isSuccess)
        assertEquals(ReportStatus.RECUPERADO, repository.getReport(ItemId("lr-laptop"))?.status)
        assertEquals(listOf(AuditAction.REPORT_CLOSED), audit.actions)
    }

    @Test
    fun onlySearchingReportsOfTheOwnerCanBeClosed() = runTest {
        val close = CloseReportUseCase(repository, session, RecordingAudit())
        assertEquals(CloseReportUseCase.NOT_CLOSABLE, close(ItemId("lr-lentes"), CloseReason.STOP_SEARCHING).exceptionOrNull()?.message)

        val stranger = CloseReportUseCase(repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.ESTUDIANTE))), RecordingAudit())
        assertEquals(CloseReportUseCase.NOT_OWNER, stranger(ItemId("lr-laptop"), CloseReason.STOP_SEARCHING).exceptionOrNull()?.message)
        assertEquals(ReportStatus.BUSCANDO, repository.getReport(ItemId("lr-laptop"))?.status)
    }
}
