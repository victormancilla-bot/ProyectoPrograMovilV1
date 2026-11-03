package bo.edu.ucb.encuentra.feature.inventory

import bo.edu.ucb.encuentra.core.InMemoryNotifications
import bo.edu.ucb.encuentra.core.RecordingNotifier
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.usecase.item.ChangeItemStatusUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.feature.claims.Audit
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionAction
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.CheckRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ResolveExpiredItemUseCase
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionEvent
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RetentionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = InMemoryRetention(listOf(itemExpiring("fi-paraguas", -5), itemExpiring("fi-termo", 2)))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): RetentionViewModel {
        val getCurrentUser = GetCurrentUserUseCase(FakeSession(demoUser(Role.STAFF)))
        val change = ChangeItemStatusUseCase(repository, Audit(), Direct, getCurrentUser)
        return RetentionViewModel(
            ObserveRetentionUseCase(repository, getCurrentUser, FixedClock),
            ResolveExpiredItemUseCase(repository, DonateItemUseCase(change), DiscardItemUseCase(change), getCurrentUser, FixedClock),
            CheckRetentionUseCase(repository, NotificationCenter(InMemoryNotifications(), RecordingNotifier(), getCurrentUser), FixedClock, BoliviaTime),
            FixedClock
        )
    }

    @Test
    fun donatingAskForConfirmationAndRemovesTheItemFromTheList() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val paraguas = vm.state.value.overview.expired.single()

        vm.handleEvent(RetentionEvent.OnRequest(paraguas, RetentionAction.DONATE))
        vm.handleEvent(RetentionEvent.OnNoteChanged("Cáritas"))
        advanceUntilIdle()
        assertEquals(ItemStatus.DISPONIBLE, repository.findItem("fi-paraguas")?.status)

        vm.handleEvent(RetentionEvent.OnConfirm)
        advanceUntilIdle()
        assertEquals(ItemStatus.DONADO, repository.findItem("fi-paraguas")?.status)
        assertEquals("\"Calculadora Casio\" quedó como donado.", vm.state.value.message)
        assertNull(vm.state.value.pending)
        assertEquals(emptyList(), vm.state.value.overview.expired)
        assertEquals(listOf("fi-termo"), vm.state.value.overview.expiring.map { it.id.value })
    }

    @Test
    fun checkNowReportsWhoWasNotified() = runTest(dispatcher) {
        val vm = viewModel()
        vm.handleEvent(RetentionEvent.OnCheckNow)
        advanceUntilIdle()
        assertEquals("Se avisó a 2 personas del staff: 1 vencido y 1 por vencer.", vm.state.value.message)
    }
}
