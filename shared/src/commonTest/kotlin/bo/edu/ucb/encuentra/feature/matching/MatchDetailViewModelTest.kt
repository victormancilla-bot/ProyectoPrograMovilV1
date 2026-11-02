package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.repository.ClaimsNotAvailableException
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.data.PendingClaimRequester
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ClaimMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMatchDetailUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RejectMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailEffect
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailEvent
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import bo.edu.ucb.encuentra.feature.myreports.mateo
import bo.edu.ucb.encuentra.feature.reportwizard.FakeMatchingTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val repository = SeedMatchRepository()

    private fun viewModel(matchId: String, requester: ClaimRequester = RecordingClaimRequester()): MatchDetailViewModel {
        val user = GetCurrentUserUseCase(FakeSession(mateo))
        return MatchDetailViewModel(
            ItemId(matchId),
            ObserveMatchDetailUseCase(repository, user),
            ClaimMatchUseCase(requester, user),
            RejectMatchUseCase(repository, FakeMatchingTrigger())
        )
    }

    @Test
    fun loadsTheDetail() = runTest(dispatcher) {
        val vm = viewModel("m-calculadora")
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(92, state.detail?.score)
        assertTrue(state.canAct)
    }

    @Test
    fun unknownMatchShowsNotFound() = runTest(dispatcher) {
        val vm = viewModel("m-no-existe")
        advanceUntilIdle()
        assertTrue(vm.state.value.notFound)
    }

    @Test
    fun claimShowsTheClaimNumberAndHidesTheActions() = runTest(dispatcher) {
        val requester = RecordingClaimRequester(number = 1026)
        val vm = viewModel("m-termo", requester)
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()

        assertEquals(1026, vm.state.value.claimNumber)
        assertFalse(vm.state.value.canAct)
        assertEquals(1, requester.requests.size)

        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()
        assertEquals(1, requester.requests.size)
    }

    @Test
    fun claimWhileV02IsPendingShowsAMessage() = runTest(dispatcher) {
        val vm = viewModel("m-termo", PendingClaimRequester())
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()

        assertEquals(ClaimsNotAvailableException().message, vm.state.value.actionError)
        assertNull(vm.state.value.claimNumber)
        assertFalse(vm.state.value.working)
    }

    @Test
    fun rejectAsksForConfirmationThenDiscardsAndGoesBack() = runTest(dispatcher) {
        val vm = viewModel("m-termo")
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnReject)
        assertTrue(vm.state.value.confirmingReject)
        vm.handleEvent(MatchDetailEvent.OnDismissReject)
        assertFalse(vm.state.value.confirmingReject)
        assertTrue(repository.rejected.isEmpty())

        val effect = async { vm.effects.first() }
        vm.handleEvent(MatchDetailEvent.OnReject)
        vm.handleEvent(MatchDetailEvent.OnConfirmReject)
        advanceUntilIdle()

        assertEquals(listOf(ItemId("m-termo")), repository.rejected)
        assertEquals(MatchDetailEffect.NavigateBack, effect.await())
    }

    @Test
    fun confirmedMatchCannotBeRejected() = runTest(dispatcher) {
        val vm = viewModel("m-cuaderno")
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnReject)
        assertFalse(vm.state.value.confirmingReject)
        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()
        assertNull(vm.state.value.claimNumber)
    }
}
