package bo.edu.ucb.encuentra.feature.matching

import bo.edu.ucb.encuentra.core.domain.repository.DuplicateClaimException
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.AnswerControlQuestionsUseCase
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
            RejectMatchUseCase(repository, FakeMatchingTrigger()),
            AnswerControlQuestionsUseCase(requester)
        )
    }

    /** Mateo ya reclamó el termo y el reclamo está pendiente de respuestas (V01). */
    private fun pendingTermoClaim(failedAttempts: Int = 0) {
        repository.claims.value = SeedData.claims + SeedData.claims.first().copy(
            id = ItemId("c-1027"), number = 1027, foundItemId = ItemId("fi-termo"), claimantId = "u-mateo",
            status = ClaimStatus.PENDIENTE, controlAnswers = emptyList(), failedAttempts = failedAttempts
        )
    }

    @Test
    fun pendingClaimAsksTheControlQuestions() = runTest(dispatcher) {
        pendingTermoClaim()
        val requester = RecordingClaimRequester()
        val vm = viewModel("m-termo", requester)
        advanceUntilIdle()

        assertEquals(listOf("¿Tiene algún sticker o marca?"), vm.state.value.detail?.existingClaim?.questions)
        assertEquals(listOf(""), vm.state.value.answers)
        assertFalse(vm.state.value.canSubmitAnswers)

        vm.handleEvent(MatchDetailEvent.OnAnswerChanged(0, "Un sticker de la UCB"))
        vm.handleEvent(MatchDetailEvent.OnSubmitAnswers)
        advanceUntilIdle()

        assertEquals(listOf(ItemId("c-1027") to listOf("Un sticker de la UCB")), requester.answers)
        assertNull(vm.state.value.answersFeedback)
    }

    @Test
    fun wrongAnswersShowTheAttemptsLeftWithoutSayingWhich() = runTest(dispatcher) {
        pendingTermoClaim()
        val requester = RecordingClaimRequester(answerResult = { id ->
            SeedData.claims.first().copy(id = id, status = ClaimStatus.PENDIENTE, failedAttempts = 2)
        })
        val vm = viewModel("m-termo", requester)
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnAnswerChanged(0, "Ninguno"))
        vm.handleEvent(MatchDetailEvent.OnSubmitAnswers)
        advanceUntilIdle()

        assertEquals("Tus respuestas no coinciden con lo registrado. Te queda 1 intento.", vm.state.value.answersFeedback)
        // Lo escrito se conserva para corregirlo.
        assertEquals(listOf("Ninguno"), vm.state.value.answers)
        assertFalse(vm.state.value.working)
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
    fun claimRefusedByV02ShowsItsMessage() = runTest(dispatcher) {
        val vm = viewModel("m-termo", FailingClaimRequester())
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()

        assertEquals(DuplicateClaimException(1025).message, vm.state.value.actionError)
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

    @Test
    fun unexpectedClaimErrorShowsAGenericMessage() = runTest(dispatcher) {
        val vm = viewModel("m-termo", FailingClaimRequester(IllegalStateException("SQLite busy")))
        advanceUntilIdle()

        vm.handleEvent(MatchDetailEvent.OnClaim)
        advanceUntilIdle()

        assertEquals(MatchDetailViewModel.CLAIM_ERROR, vm.state.value.actionError)
    }
}
