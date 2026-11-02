package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.core.domain.repository.ClaimsNotAvailableException
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.data.PendingClaimRequester
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.matching.domain.model.ComparisonVerdict.COINCIDE
import bo.edu.ucb.encuentra.feature.matching.domain.model.ComparisonVerdict.POR_VERIFICAR
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ClaimMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMatchDetailUseCase
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Guarda quién pidió el reclamo y devuelve un número fijo, como lo haría V02. */
internal class RecordingClaimRequester(private val number: Int = 1026) : ClaimRequester {
    val requests = mutableListOf<Pair<ItemId, String>>()
    override suspend fun requestClaim(matchId: ItemId, claimantId: String): Int {
        requests += matchId to claimantId
        return number
    }
}

class MatchDetailUseCasesTest {

    private val repository = SeedMatchRepository()
    private val observe = ObserveMatchDetailUseCase(repository, GetCurrentUserUseCase(FakeSession(mateo)))

    @Test
    fun calculatorMatchComparesEveryFieldAsMatching() = runTest {
        val detail = assertNotNull(observe(ItemId("m-calculadora")).first())

        assertEquals(92, detail.score)
        assertEquals("Calculadora científica", detail.report.title)
        assertEquals("Calculadora Científica Casio", detail.item.title)
        assertEquals(CampusZone.BLOQUE_C, detail.item.zone)
        with(detail.comparison) {
            assertEquals(listOf(COINCIDE, COINCIDE, COINCIDE, COINCIDE, COINCIDE), listOf(type, brand, color, zone, date))
        }
        assertTrue(detail.canAct)
    }

    @Test
    fun differentDataIsMarkedToVerify() = runTest {
        val detail = assertNotNull(observe(ItemId("m-termo")).first())
        with(detail.comparison) {
            assertEquals(listOf(POR_VERIFICAR, POR_VERIFICAR, POR_VERIFICAR, POR_VERIFICAR), listOf(type, brand, color, zone))
        }
    }

    @Test
    fun confirmedMatchOrClosedReportCannotBeClaimedOrRejected() = runTest {
        assertFalse(assertNotNull(observe(ItemId("m-cuaderno")).first()).canAct)

        repository.groups.value = repository.groups.value.map { group ->
            if (group.report.id.value == "lr-calculadora") group.copy(report = group.report.copy(status = ReportStatus.CERRADO)) else group
        }
        assertFalse(assertNotNull(observe(ItemId("m-calculadora")).first()).canAct)
    }

    @Test
    fun matchOfAnotherUserOrDiscardedOrMissingShowsNothing() = runTest {
        val otherUser = ObserveMatchDetailUseCase(repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.ESTUDIANTE))))
        assertNull(otherUser(ItemId("m-calculadora")).first())
        assertNull(ObserveMatchDetailUseCase(repository, GetCurrentUserUseCase(FakeSession(null)))(ItemId("m-calculadora")).first())
        assertNull(observe(ItemId("m-no-existe")).first())

        repository.groups.value = repository.groups.value.map { group ->
            group.copy(candidates = group.candidates.map {
                if (it.match.id.value == "m-termo") it.copy(match = it.match.copy(status = MatchStatus.DESCARTADO)) else it
            })
        }
        assertNull(observe(ItemId("m-termo")).first())
    }

    @Test
    fun claimAsksV02ForTheCurrentUser() = runTest {
        val requester = RecordingClaimRequester()
        val result = ClaimMatchUseCase(requester, GetCurrentUserUseCase(FakeSession(mateo)))(ItemId("m-termo"))

        assertEquals(1026, result.getOrNull())
        assertEquals(listOf(ItemId("m-termo") to "u-mateo"), requester.requests)
    }

    @Test
    fun claimFailsWithoutSessionOrWhileV02IsPending() = runTest {
        val noSession = ClaimMatchUseCase(RecordingClaimRequester(), GetCurrentUserUseCase(FakeSession(null)))
        assertTrue(noSession(ItemId("m-termo")).isFailure)

        val pending = ClaimMatchUseCase(PendingClaimRequester(), GetCurrentUserUseCase(FakeSession(mateo)))
        assertIs<ClaimsNotAvailableException>(pending(ItemId("m-termo")).exceptionOrNull())
    }
}
