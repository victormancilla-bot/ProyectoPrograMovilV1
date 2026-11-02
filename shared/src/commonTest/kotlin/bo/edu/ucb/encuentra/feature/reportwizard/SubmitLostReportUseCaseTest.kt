package bo.edu.ucb.encuentra.feature.reportwizard

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.TimeSlot
import bo.edu.ucb.encuentra.feature.reportwizard.domain.repository.LostReportRepository
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.SubmitLostReportUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

internal class FakeLostReportRepository : LostReportRepository {
    val saved = mutableListOf<LostReport>()
    override suspend fun save(report: LostReport) {
        saved += report
    }
}

internal class FakeMatchingTrigger(private val fail: Boolean = false) : MatchingTrigger {
    val triggered = mutableListOf<LostReport>()
    val registered = mutableListOf<FoundItem>()
    override suspend fun onReportCreated(report: LostReport) {
        triggered += report
        if (fail) error("matching caído")
    }
    override suspend fun onReportUpdated(report: LostReport) {
        triggered += report
    }
    override suspend fun onFoundItemRegistered(item: FoundItem) {
        registered += item
        if (fail) error("matching caído")
    }
}

/** Borrador completo de los pasos 1-3; "hoy" es el 02/11/2026 (FixedClock). */
internal val completeDraft = ReportDraft(
    category = Category.ELECTRONICA,
    subcategory = Subcategory.LAPTOP,
    title = " Laptop Dell XPS ",
    color = "Plateado",
    brand = " ",
    features = "Sticker de la UCB",
    zone = CampusZone.BIBLIOTECA_CENTRAL,
    locationDetail = "Piso 2",
    lostFrom = LocalDate(2026, 10, 30),
    lostTo = LocalDate(2026, 11, 2),
    timeSlot = TimeSlot.MANANA,
    photoUri = "file:///foto.jpg",
    contactEmail = "estudiante@ucb.edu.bo",
    contactPhone = "+591 7123-4567"
)

class SubmitLostReportUseCaseTest {

    private val repository = FakeLostReportRepository()

    private fun submit(matching: MatchingTrigger = FakeMatchingTrigger(), loggedIn: Boolean = true) =
        SubmitLostReportUseCase(
            ValidateReportStepUseCase(FixedClock, BoliviaTime),
            repository,
            matching,
            GetCurrentUserUseCase(FakeSession(if (loggedIn) demoUser(Role.ESTUDIANTE) else null)),
            FixedClock
        )

    @Test
    fun savesTheReportAsSearchingAndTriggersMatching() = runTest {
        val matching = FakeMatchingTrigger()
        val report = submit(matching)(completeDraft).getOrThrow()

        assertEquals(listOf(report), repository.saved)
        assertEquals(listOf(report), matching.triggered)
        assertEquals(ReportStatus.BUSCANDO, report.status)
        assertEquals("Laptop Dell XPS", report.title)
        assertNull(report.brand)
        assertEquals("Piso 2", report.locationDetail)
        assertEquals("+59171234567", report.contactPhone)
        assertEquals("u-ana-staff", report.ownerId)
        assertEquals(Instant.parse("2026-10-30T06:00:00-04:00"), report.lostFrom)
        assertEquals(Instant.parse("2026-11-02T12:00:00-04:00"), report.lostTo)
        assertEquals(FixedClock.now(), report.createdAt)
    }

    @Test
    fun incompleteDraftIsNotSaved() = runTest {
        val result = submit()(completeDraft.copy(timeSlot = null))
        assertEquals(ReportFieldError.TIME_SLOT_REQUIRED.message, result.exceptionOrNull()?.message)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun matchingFailureDoesNotLoseTheReport() = runTest {
        assertTrue(submit(FakeMatchingTrigger(fail = true))(completeDraft).isSuccess)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun needsASession() = runTest {
        assertEquals(SubmitLostReportUseCase.NO_SESSION, submit(loggedIn = false)(completeDraft).exceptionOrNull()?.message)
    }
}
