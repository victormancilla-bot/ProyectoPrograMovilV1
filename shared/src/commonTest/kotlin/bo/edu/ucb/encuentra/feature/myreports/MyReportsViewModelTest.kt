package bo.edu.ucb.encuentra.feature.myreports

import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportFilter
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.CloseReportUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveMyReportsUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveReportDetailUseCase
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsEffect
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsEvent
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsViewModel
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
class MyReportsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeMyReportsRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(loggedIn: Boolean = true): MyReportsViewModel {
        val session = GetCurrentUserUseCase(FakeSession(if (loggedIn) mateo else null))
        return MyReportsViewModel(
            ObserveMyReportsUseCase(repository, session),
            ObserveReportDetailUseCase(repository),
            CloseReportUseCase(repository, session, RecordingAudit())
        )
    }

    @Test
    fun listLoadsFromTheRepositoryAndFiltersByStatus() = runTest(dispatcher) {
        val vm = viewModel()
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        assertEquals(5, vm.state.value.visibleReports.size)

        vm.handleEvent(MyReportsEvent.OnFilterSelected(ReportFilter.BUSCANDO))
        assertEquals(listOf("Laptop Dell XPS"), vm.state.value.visibleReports.map { it.title })
        vm.handleEvent(MyReportsEvent.OnFilterSelected(ReportFilter.VERIFICACION))
        assertEquals(3, vm.state.value.visibleReports.size)
    }

    @Test
    fun withoutReportsTheEmptyStateShows() = runTest(dispatcher) {
        val vm = viewModel(loggedIn = false)
        advanceUntilIdle()
        assertTrue(vm.state.value.isEmpty)
    }

    @Test
    fun detailShowsTheReportWithItsMatchesAndBackReturnsToTheList() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(MyReportsEvent.OnOpenReport(ItemId("lr-calculadora")))
        advanceUntilIdle()
        assertEquals(listOf(92, 65, 30), vm.state.value.detail?.matches?.map { it.score })
        assertFalse(vm.state.value.canClose)

        vm.handleEvent(MyReportsEvent.OnBackFromDetail)
        assertFalse(vm.state.value.isDetailOpen)
        assertNull(vm.state.value.detail)
    }

    @Test
    fun closingASearchingReportAsksForConfirmationAndUpdatesTheList() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(MyReportsEvent.OnOpenReport(ItemId("lr-laptop")))
        advanceUntilIdle()
        assertTrue(vm.state.value.canClose)

        vm.handleEvent(MyReportsEvent.OnCloseReportClicked)
        assertTrue(vm.state.value.showCloseDialog)
        vm.handleEvent(MyReportsEvent.OnConfirmClose(CloseReason.STOP_SEARCHING))
        advanceUntilIdle()

        assertFalse(vm.state.value.showCloseDialog)
        assertEquals(ReportStatus.CERRADO, vm.state.value.detail?.report?.status)
        assertFalse(vm.state.value.canClose)
        // La lista se actualizó sola: ya no hay reportes "Buscando".
        vm.handleEvent(MyReportsEvent.OnFilterSelected(ReportFilter.BUSCANDO))
        assertTrue(vm.state.value.visibleReports.isEmpty())
    }

    @Test
    fun closeIsNotOfferedForReportsInVerification() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(MyReportsEvent.OnOpenReport(ItemId("lr-lentes")))
        advanceUntilIdle()
        vm.handleEvent(MyReportsEvent.OnCloseReportClicked)
        assertFalse(vm.state.value.showCloseDialog)
    }

    @Test
    fun matchesNavigate() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val effect = async { vm.effects.first() }
        advanceUntilIdle()
        vm.handleEvent(MyReportsEvent.OnMatchClicked(ItemId("m-calculadora")))
        advanceUntilIdle()
        assertEquals(MyReportsEffect.NavigateToMatchDetail("m-calculadora"), effect.await())
    }
}
