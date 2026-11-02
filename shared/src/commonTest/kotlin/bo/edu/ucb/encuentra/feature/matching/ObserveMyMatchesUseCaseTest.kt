package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchCandidate
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchPair
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMyMatchesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Arma los grupos desde el seed 2026 (los mismos datos que ve Mateo en la app). */
internal class SeedMatchRepository : MatchRepository {
    val groups = MutableStateFlow(
        SeedData.lostReports.filter { it.ownerId == "u-mateo" }.map { report ->
            ReportMatches(
                report = report,
                candidates = SeedData.matches.filter { it.lostReportId == report.id }.map { match ->
                    MatchCandidate(match, SeedData.foundItems.first { it.id == match.foundItemId })
                }
            )
        }
    )

    override fun observeForOwner(ownerId: String): Flow<List<ReportMatches>> = groups

    override fun observeMatch(matchId: ItemId): Flow<MatchPair?> = groups.map { all ->
        all.firstNotNullOfOrNull { group ->
            group.candidates.firstOrNull { it.match.id == matchId }?.let { MatchPair(it.match, group.report, it.item) }
        }
    }

    val rejected = mutableListOf<ItemId>()

    override suspend fun reject(matchId: ItemId): LostReport? {
        rejected += matchId
        return SeedData.lostReports.first()
    }
}

class ObserveMyMatchesUseCaseTest {

    private val repository = SeedMatchRepository()
    private val observe = ObserveMyMatchesUseCase(repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.ESTUDIANTE))))

    @Test
    fun seedShowsTheCalculatorFirstWithItsCandidatesRanked() = runTest {
        val groups = observe().first()
        assertEquals(listOf("lr-calculadora", "lr-cuaderno"), groups.map { it.report.id.value })
        assertEquals(listOf(92, 65, 30), groups.first().candidates.map { it.match.score })
        assertEquals("Calculadora Científica Casio", groups.first().candidates.first().item.title)
    }

    @Test
    fun reportsWithoutCandidatesAndDiscardedMatchesAreHidden() = runTest {
        val groups = repository.groups.value
        repository.groups.value = groups.map { group ->
            group.copy(candidates = group.candidates.map { candidate ->
                if (candidate.match.score == 92 && group.report.id.value == "lr-calculadora") {
                    candidate.copy(match = candidate.match.copy(status = MatchStatus.DESCARTADO))
                } else candidate
            })
        }
        val result = observe().first()
        assertEquals(listOf(65, 30), result.first().candidates.map { it.match.score })
        assertTrue(result.none { it.candidates.isEmpty() })
    }

    @Test
    fun closedReportsNoLongerShowTheirCandidates() = runTest {
        repository.groups.value = repository.groups.value.map { group ->
            if (group.report.id.value == "lr-calculadora") group.copy(report = group.report.copy(status = ReportStatus.CERRADO)) else group
        }
        assertEquals(listOf("lr-cuaderno"), observe().first().map { it.report.id.value })
    }

    @Test
    fun withoutSessionThereAreNoMatches() = runTest {
        val noSession = ObserveMyMatchesUseCase(repository, GetCurrentUserUseCase(FakeSession(null)))
        assertTrue(noSession().first().isEmpty())
    }
}

private fun Match.copy(status: MatchStatus) = Match(id, lostReportId, foundItemId, score, status, createdAt)
