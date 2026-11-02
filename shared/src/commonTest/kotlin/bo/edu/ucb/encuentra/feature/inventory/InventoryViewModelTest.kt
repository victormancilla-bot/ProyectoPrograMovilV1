package bo.edu.ucb.encuentra.feature.inventory

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.usecase.item.MoveCustodyUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryFilter
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryItemUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryUseCase
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryEffect
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryEvent
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertTrue

/** H05 sobre el mismo inventario en memoria: mover la custodia se ve en el detalle y el historial. */
private class Lifecycle(private val inventory: SeedInventoryRepository) : FoundItemLifecycleRepository {
    override suspend fun getById(id: ItemId) = inventory.items.value.firstOrNull { it.id == id }
    override suspend fun updateStatus(id: ItemId, status: ItemStatus) = inventory.set(id.value) { it.copy(status = status) }
    override suspend fun moveCustody(event: CustodyEvent) {
        inventory.history.value = inventory.history.value + event
        inventory.set(event.foundItemId.value) { it.copy(custodyPlace = event.place, custodyDetail = event.detail) }
    }
    override fun observeCustodyHistory(id: ItemId) = inventory.history.map { all -> all.filter { it.foundItemId == id } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val repository = SeedInventoryRepository()
    private val audit = mutableListOf<Pair<AuditAction, String>>()

    private fun viewModel(): InventoryViewModel {
        val user = GetCurrentUserUseCase(FakeSession(demoUser(Role.STAFF)))
        val move = MoveCustodyUseCase(
            Lifecycle(repository),
            object : AuditRecorder {
                override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
                    audit += action to detail
                }
            },
            object : TransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T = block()
            },
            user,
            FixedClock
        )
        return InventoryViewModel(
            ObserveInventoryUseCase(repository, FixedClock),
            ObserveInventoryItemUseCase(repository, FixedClock),
            move,
            user
        )
    }

    @Test
    fun listsTheInventoryAndFiltersWithChipsAndSearch() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("Sede La Paz", vm.state.value.campus)
        assertEquals(6, vm.state.value.visible.size)

        vm.handleEvent(InventoryEvent.OnFilterSelected(InventoryFilter.RECLAMADO))
        assertEquals(3, vm.state.value.visible.size)

        vm.handleEvent(InventoryEvent.OnQueryChanged("casio"))
        assertEquals(listOf("fi-calculadora"), vm.state.value.visible.map { it.item.id.value })
    }

    @Test
    fun opensAndClosesTheDetail() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.handleEvent(InventoryEvent.OnItemClicked("fi-llavero"))
        advanceUntilIdle()
        assertEquals("Llavero con 3 llaves", vm.state.value.detail?.entry?.item?.title)

        vm.handleEvent(InventoryEvent.OnCloseDetail)
        assertNull(vm.state.value.selectedId)
        assertNull(vm.state.value.detail)
    }

    @Test
    fun editingCustodyMovesTheItemAndAddsItToTheHistory() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(InventoryEvent.OnItemClicked("fi-termo"))
        advanceUntilIdle()
        val before = vm.state.value.detail!!.history.size

        vm.handleEvent(InventoryEvent.OnEditCustody)
        assertEquals(CustodyPlace.OFICINA, vm.state.value.custodyDraft?.place)
        vm.handleEvent(InventoryEvent.OnCustodyPlaceSelected(CustodyPlace.BODEGA))
        vm.handleEvent(InventoryEvent.OnCustodyDetailChanged("Estante B"))
        vm.handleEvent(InventoryEvent.OnCustodyNoteChanged("Limpieza de la oficina"))
        vm.handleEvent(InventoryEvent.OnSaveCustody)
        advanceUntilIdle()

        val detail = vm.state.value.detail!!
        assertEquals(CustodyPlace.BODEGA, detail.entry.item.custodyPlace)
        assertEquals("Estante B", detail.entry.item.custodyDetail)
        assertEquals(before + 1, detail.history.size)
        assertEquals("Limpieza de la oficina", detail.history.first().event.note)
        assertEquals(AuditAction.CUSTODY_CHANGED to "Oficina → Bodega", audit.single())
        assertNull(vm.state.value.custodyDraft)
        assertEquals(InventoryViewModel.CUSTODY_SAVED, vm.state.value.info)
    }

    @Test
    fun savingTheSamePlaceShowsTheError() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.handleEvent(InventoryEvent.OnItemClicked("fi-termo"))
        advanceUntilIdle()

        vm.handleEvent(InventoryEvent.OnEditCustody)
        vm.handleEvent(InventoryEvent.OnSaveCustody)
        advanceUntilIdle()

        assertEquals(ItemErrors.SAME_PLACE, vm.state.value.actionError)
        assertTrue(vm.state.value.custodyDraft != null)
    }

    @Test
    fun registerFoundNavigatesToTheForm() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val effect = async { vm.effects.first() }
        vm.handleEvent(InventoryEvent.OnRegisterFound)
        advanceUntilIdle()
        assertEquals(InventoryEffect.NavigateToFoundForm, effect.await())
    }
}
