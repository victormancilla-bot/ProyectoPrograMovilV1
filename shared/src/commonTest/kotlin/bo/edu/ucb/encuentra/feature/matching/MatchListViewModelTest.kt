package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMyMatchesUseCase
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListEffect
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListEvent
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertTrue

/** Falla la primera vez y responde con el seed después (para probar "Reintentar"). */
private class FlakyMatchRepository : MatchRepository {
    var calls = 0
    private val seed = SeedMatchRepository()
    override fun observeForOwner(ownerId: String): Flow<List<ReportMatches>> = flow {
        calls++
        if (calls == 1) error("Room no disponible")
        emit(seed.groups.value)
    }

    override suspend fun reject(matchId: ItemId) = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class MatchListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: MatchRepository) =
        MatchListViewModel(ObserveMyMatchesUseCase(repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.ESTUDIANTE)))))

    @Test
    fun startsLoadingAndShowsTheBestReportFirst() = runTest(dispatcher) {
        val vm = viewModel(SeedMatchRepository())
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(ItemId("lr-calculadora"), state.selectedReportId)
        assertEquals(listOf(92, 65, 30), state.selected?.candidates?.map { it.match.score })
    }

    @Test
    fun choosingAnotherReportShowsItsCandidates() = runTest(dispatcher) {
        val vm = viewModel(SeedMatchRepository())
        advanceUntilIdle()
        vm.handleEvent(MatchListEvent.OnReportSelected(ItemId("lr-cuaderno")))
        assertEquals("Cuaderno de Tapa Dura", vm.state.value.selected?.candidates?.single()?.item?.title)
    }

    @Test
    fun noMatchesIsTheEmptyState() = runTest(dispatcher) {
        val repository = SeedMatchRepository().apply { groups.value = groups.value.map { it.copy(candidates = emptyList()) } }
        val vm = viewModel(repository)
        advanceUntilIdle()
        assertTrue(vm.state.value.isEmpty)
        assertNull(vm.state.value.selected)
    }

    @Test
    fun errorCanBeRetried() = runTest(dispatcher) {
        val repository = FlakyMatchRepository()
        val vm = viewModel(repository)
        advanceUntilIdle()
        assertEquals(MatchListViewModel.LOAD_ERROR, vm.state.value.error)

        vm.handleEvent(MatchListEvent.OnRetry)
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(2, vm.state.value.groups.size)
    }

    @Test
    fun cardAndModifyCriteriaNavigate() = runTest(dispatcher) {
        val vm = viewModel(SeedMatchRepository())
        advanceUntilIdle()
        val effects = async { vm.effects.first() }
        advanceUntilIdle()
        vm.handleEvent(MatchListEvent.OnMatchClicked(ItemId("m-calculadora")))
        advanceUntilIdle()
        assertEquals(MatchListEffect.NavigateToMatchDetail("m-calculadora"), effects.await())

        val next = async { vm.effects.first() }
        advanceUntilIdle()
        vm.handleEvent(MatchListEvent.OnModifyCriteria)
        advanceUntilIdle()
        assertEquals(MatchListEffect.NavigateToMyReports, next.await())
    }
}
