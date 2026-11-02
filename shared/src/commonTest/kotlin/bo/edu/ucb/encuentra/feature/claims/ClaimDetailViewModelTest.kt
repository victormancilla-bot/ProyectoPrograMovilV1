package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveOpenClaimsUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDecision
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEffect
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
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
class ClaimDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = SeedClaimStore()
    private val audit = Audit()

    private fun viewModel(claimId: String, user: UserModel = staff): ClaimDetailViewModel {
        val current = GetCurrentUserUseCase(FakeSession(user))
        val change = ChangeClaimStatusUseCase(store, ItemsOf(store), audit, Direct, current, FixedClock)
        return ClaimDetailViewModel(
            ItemId(claimId),
            ObserveClaimReviewUseCase(store, current),
            ApproveClaimUseCase(change),
            RejectClaimUseCase(change),
            FixedClock
        )
    }

    @Test
    fun staffSeesTheAnswersWithSuggestions() = runTest(dispatcher) {
        val vm = viewModel("c-1024")
        advanceUntilIdle()

        val review = vm.state.value.review!!
        assertEquals(2, review.matchingAnswers)
        assertTrue(review.canApprove)
    }

    @Test
    fun aStudentCannotOpenTheReview() = runTest(dispatcher) {
        val vm = viewModel("c-1024", user = mateo)
        advanceUntilIdle()
        assertTrue(vm.state.value.notFound)
    }

    @Test
    fun approvingNeedsConfirmationAndKeepsTheNote() = runTest(dispatcher) {
        val vm = viewModel("c-1024")
        advanceUntilIdle()

        vm.handleEvent(ClaimDetailEvent.OnDecide(ClaimDecision.APPROVE))
        assertEquals(ClaimDecision.APPROVE, vm.state.value.confirming)
        vm.handleEvent(ClaimDetailEvent.OnDismiss)
        assertEquals(ClaimStatus.EN_VERIFICACION, store.claim(1024).status)

        vm.handleEvent(ClaimDetailEvent.OnDecide(ClaimDecision.APPROVE))
        // Sin motivo no se confirma: el diálogo sigue abierto y avisa.
        vm.handleEvent(ClaimDetailEvent.OnConfirm)
        advanceUntilIdle()
        assertTrue(vm.state.value.reasonMissing)
        assertEquals(ClaimDecision.APPROVE, vm.state.value.confirming)
        assertEquals(ClaimStatus.EN_VERIFICACION, store.claim(1024).status)

        vm.handleEvent(ClaimDetailEvent.OnNoteChanged("Mostró su carnet"))
        vm.handleEvent(ClaimDetailEvent.OnConfirm)
        advanceUntilIdle()

        assertEquals(ClaimStatus.APROBADO, store.claim(1024).status)
        assertEquals("Reclamo #1024 · En verificación → Aprobado · Mostró su carnet", audit.entries.last().second)
        assertNull(vm.state.value.confirming)
        assertFalse(vm.state.value.working)
    }

    @Test
    fun aPendingClaimCanOnlyBeRejected() = runTest(dispatcher) {
        val vm = viewModel("c-1025")
        advanceUntilIdle()

        vm.handleEvent(ClaimDetailEvent.OnDecide(ClaimDecision.APPROVE))
        assertNull(vm.state.value.confirming)

        vm.handleEvent(ClaimDetailEvent.OnDecide(ClaimDecision.REJECT))
        vm.handleEvent(ClaimDetailEvent.OnNoteChanged("No respondió las preguntas"))
        vm.handleEvent(ClaimDetailEvent.OnConfirm)
        advanceUntilIdle()
        assertEquals(ClaimStatus.RECHAZADO, store.claim(1025).status)
    }

    @Test
    fun theListOpensTheDetail() = runTest(dispatcher) {
        val vm = ClaimsViewModel(ObserveOpenClaimsUseCase(store, GetCurrentUserUseCase(FakeSession(staff))), FixedClock)
        advanceUntilIdle()
        assertEquals(listOf(1024, 1025, 1026), vm.state.value.claims.map { it.claim.number })

        vm.handleEvent(ClaimsEvent.OnToggle("c-1024"))
        assertEquals(setOf("c-1024"), vm.state.value.expanded)
        vm.handleEvent(ClaimsEvent.OnToggle("c-1024"))
        assertTrue(vm.state.value.expanded.isEmpty())

        val effect = async { vm.effects.first() }
        vm.handleEvent(ClaimsEvent.OnClaimClicked("c-1024"))
        advanceUntilIdle()
        assertEquals(ClaimsEffect.NavigateToDetail("c-1024"), effect.await())
    }
}
