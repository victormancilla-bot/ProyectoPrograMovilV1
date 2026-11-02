package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerComparator
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict.COINCIDE
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict.NO_COINCIDE
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict.SIN_REFERENCIA
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.model.InvalidClaimTransitionException
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveOpenClaimsUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnswerComparatorTest {

    @Test
    fun toleratesCaseAccentsAndFillerWords() {
        assertEquals(COINCIDE, AnswerComparator.compare("cafe", "Café"))
        assertEquals(COINCIDE, AnswerComparator.compare("  MATEO quispe ", "Mateo Quispe"))
        assertEquals(COINCIDE, AnswerComparator.compare("uno de la UCB", "Escudo de la UCB"))
        assertEquals(COINCIDE, AnswerComparator.compare("Carnet universitario y una tarjeta de débito", "Carnet universitario y tarjeta de débito"))
        assertEquals(COINCIDE, AnswerComparator.compare("Es una Casio fx-991 ES Plus", "fx-991"))
    }

    @Test
    fun differentAnswersDoNotMatch() {
        assertEquals(NO_COINCIDE, AnswerComparator.compare("Rojo", "Café"))
        assertEquals(NO_COINCIDE, AnswerComparator.compare("Luis Mendoza", "Mateo Quispe"))
        assertEquals(NO_COINCIDE, AnswerComparator.compare("   ", "Café"))
        // Una sola letra no "contiene" la respuesta.
        assertEquals(NO_COINCIDE, AnswerComparator.compare("a", "Café"))
    }

    @Test
    fun withoutExpectedAnswerOnlyTheStaffDecides() {
        assertEquals(SIN_REFERENCIA, AnswerComparator.compare("Lo que sea", null))
        assertEquals(SIN_REFERENCIA, AnswerComparator.compare("Lo que sea", " "))
    }
}

class ControlQuestionsTest {

    private val store = SeedClaimStore()
    private val audit = Audit()
    private val session = FakeSession(mateo)
    private val user = GetCurrentUserUseCase(session)
    private val create = CreateClaimUseCase(store, ItemsOf(store), audit, Direct, FixedClock)
    private val answer = SubmitClaimAnswersUseCase(ChangeClaimStatusUseCase(store, ItemsOf(store), audit, Direct, user, FixedClock))
    private val review = ObserveClaimReviewUseCase(store, user)

    private suspend fun llaveroClaim(): Claim = store.claim(create.requestClaim(ItemId("m-llavero"), mateo.id))

    @Test
    fun matchingAnswersGoToTheStaff() = runTest {
        val claim = llaveroClaim()

        val answered = answer(claim.id, listOf("Tiene el escudo de la UCB")).getOrThrow()

        assertEquals(ClaimStatus.EN_VERIFICACION, answered.status)
        assertEquals(0, answered.failedAttempts)
        assertEquals(listOf("Tiene el escudo de la UCB"), answered.controlAnswers)
    }

    @Test
    fun wrongAnswersCostAnAttemptWithoutRevealingWhichOne() = runTest {
        val claim = llaveroClaim()

        val retry = answer(claim.id, listOf("Un oso de peluche")).getOrThrow()

        assertEquals(ClaimStatus.PENDIENTE, retry.status)
        assertEquals(1, retry.failedAttempts)
        assertEquals(2, retry.attemptsLeft)
        assertEquals(AuditAction.CLAIM_ANSWERED to "Reclamo #1027 · respuestas no coinciden (intento 1 de 3)", audit.entries.last())
        // Después de fallar se puede acertar.
        assertEquals(ClaimStatus.EN_VERIFICACION, answer(claim.id, listOf("escudo UCB")).getOrThrow().status)
    }

    @Test
    fun threeWrongAttemptsBlockTheClaimAndReleaseTheItem() = runTest {
        val claim = llaveroClaim()
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-llavero"))

        repeat(2) { answer(claim.id, listOf("Un oso")).getOrThrow() }
        val blocked = answer(claim.id, listOf("Un oso")).getOrThrow()

        assertEquals(ClaimStatus.RECHAZADO, blocked.status)
        assertEquals(3, blocked.failedAttempts)
        assertEquals(0, blocked.attemptsLeft)
        assertEquals(ItemStatus.DISPONIBLE, store.item("fi-llavero"))
        assertEquals(MatchStatus.DESCARTADO, store.match("m-llavero"))
        assertEquals(ReportStatus.BUSCANDO, store.report("lr-calculadora"))
        assertEquals(AuditAction.CLAIM_REJECTED to "Reclamo #1027 · bloqueado: 3 intentos sin coincidir", audit.entries.last())

        // Bloqueado: ya no puede volver a responder.
        assertIs<InvalidClaimTransitionException>(answer(claim.id, listOf("Escudo de la UCB")).exceptionOrNull())
    }

    @Test
    fun everyQuestionMustBeAnsweredAndThatDoesNotCostAnAttempt() = runTest {
        // La calculadora tiene 2 preguntas.
        val claim = store.claim(create.requestClaim(ItemId("m-calculadora"), mateo.id))

        val error = answer(claim.id, listOf("Mateo Quispe")).exceptionOrNull()

        assertIs<ClaimRequestException>(error)
        assertEquals(ClaimErrors.ANSWERS_REQUIRED, error.message)
        assertEquals(0, store.claim(claim.number).failedAttempts)
    }

    @Test
    fun anItemWithoutExpectedAnswersGoesStraightToTheStaff() = runTest {
        store.items[ItemId("fi-llavero")] = store.items.getValue(ItemId("fi-llavero")).copy(expectedAnswers = emptyList())
        val claim = llaveroClaim()

        assertEquals(ClaimStatus.EN_VERIFICACION, answer(claim.id, listOf("No sé")).getOrThrow().status)
    }

    @Test
    fun theStaffSeesAnswersExpectedAnswersAndSuggestions() = runTest {
        session.setCurrentUser(staff)

        // #1024 de Ana (seed): ya respondió las dos preguntas de la billetera.
        val detail = review(ItemId("c-1024")).first()!!

        assertEquals("Ana Gómez", detail.claimantName)
        assertEquals("Billetera Negra de Cuero", detail.item.title)
        assertEquals(listOf("Carnet universitario y tarjeta de débito", "Café"), detail.answers.map { it.expected })
        assertEquals(listOf(COINCIDE, COINCIDE), detail.answers.map { it.verdict })
        assertEquals(2, detail.matchingAnswers)
        assertTrue(detail.canApprove)

        // #1025 de Luis todavía no respondió: no hay sugerencia y no se puede aprobar.
        val pending = review(ItemId("c-1025")).first()!!
        assertEquals(listOf(null, null), pending.answers.map { it.answer })
        assertTrue(!pending.canApprove && pending.canReject)
    }

    @Test
    fun theStaffSeesTheStudentReportOnlyWhenItIsTheirs() = runTest {
        val claim = llaveroClaim()
        session.setCurrentUser(staff)

        val own = review(claim.id).first()!!
        assertEquals("Calculadora científica", own.report?.title)
        assertEquals(30, own.matchScore)

        // El #1025 de Luis apunta (por error del seed) a la coincidencia de Mateo: no se muestra el reporte ajeno.
        val other = review(ItemId("c-1025")).first()!!
        assertNull(other.report)
        assertNull(other.matchScore)
    }

    @Test
    fun answersArePrivateToTheStaff() = runTest {
        assertNull(review(ItemId("c-1024")).first())
        assertTrue(ObserveOpenClaimsUseCase(store, user)().first().isEmpty())

        session.setCurrentUser(staff)
        // Primero los que ya respondieron (en verificación), después los pendientes.
        assertEquals(listOf(1024, 1025, 1026), ObserveOpenClaimsUseCase(store, user)().first().map { it.claim.number })
    }
}
