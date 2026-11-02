package bo.edu.ucb.encuentra.feature.delivery

import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.Audit
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.claims.ItemsOf
import bo.edu.ucb.encuentra.feature.claims.SeedClaimStore
import bo.edu.ucb.encuentra.feature.claims.staff
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ConfirmDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ScheduleDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryEvent
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryViewModel
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = SeedClaimStore()
    private val audit = Audit()
    private val notifier = DeliveryNotifier()
    private val user = GetCurrentUserUseCase(FakeSession(staff))
    private val change = ChangeClaimStatusUseCase(store, ItemsOf(store), audit, Direct, user, FixedClock, notifier)

    /** #1024 de Ana (seed, en verificación); approve1024() lo deja listo para entrega. */
    private fun viewModel(): DeliveryViewModel = DeliveryViewModel(
        ItemId("c-1024"),
        ObserveClaimReviewUseCase(store, user),
        ScheduleDeliveryUseCase(store, ItemsOf(store), audit, Direct, user, FixedClock, BoliviaTime, notifier),
        ConfirmDeliveryUseCase(DeliverClaimUseCase(change)),
        BoliviaTime
    )

    private suspend fun approve1024() {
        ApproveClaimUseCase(change)(ItemId("c-1024"), "Describió el interior").getOrThrow()
    }

    @Test
    fun showsTheApprovedClaimReadyForDelivery() = runTest(dispatcher) {
        approve1024()
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.readyForDelivery)
        assertEquals("Ana Gómez", state.review?.claimantName)
        assertEquals("Ana Staff", state.review?.reviewerName)
        assertFalse(state.canConfirm)
    }

    @Test
    fun schedulingNeedsDateAndTime() = runTest(dispatcher) {
        approve1024()
        val vm = viewModel()
        advanceUntilIdle()

        vm.handleEvent(DeliveryEvent.OnDatePicked(LocalDate(2026, 11, 4)))
        assertFalse(vm.state.value.canSchedule)
        vm.handleEvent(DeliveryEvent.OnTimePicked(LocalTime(14, 30)))
        vm.handleEvent(DeliveryEvent.OnSchedule)
        advanceUntilIdle()

        assertNotNull(store.claim(1024).scheduledDeliveryAt)
        assertEquals(DeliveryViewModel.SCHEDULED, vm.state.value.info)
        assertEquals(1, notifier.scheduled.size)
    }

    @Test
    fun confirmingNeedsBothChecksAndDelivers() = runTest(dispatcher) {
        approve1024()
        val vm = viewModel()
        advanceUntilIdle()

        vm.handleEvent(DeliveryEvent.OnIdentityVerified(true))
        assertFalse(vm.state.value.canConfirm)
        vm.handleEvent(DeliveryEvent.OnConfirm)
        advanceUntilIdle()
        assertEquals(ClaimStatus.APROBADO, store.claim(1024).status)

        vm.handleEvent(DeliveryEvent.OnItemReceived(true))
        assertTrue(vm.state.value.canConfirm)
        vm.handleEvent(DeliveryEvent.OnConfirm)
        advanceUntilIdle()

        assertEquals(ClaimStatus.ENTREGADO, store.claim(1024).status)
        assertEquals(ItemStatus.ENTREGADO, store.item("fi-billetera"))
    }

    @Test
    fun aClaimThatIsNotApprovedCannotBeDelivered() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.readyForDelivery)
        vm.handleEvent(DeliveryEvent.OnIdentityVerified(true))
        vm.handleEvent(DeliveryEvent.OnItemReceived(true))
        assertFalse(vm.state.value.canConfirm)
    }
}
