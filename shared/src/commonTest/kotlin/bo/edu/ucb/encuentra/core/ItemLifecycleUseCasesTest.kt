package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.InvalidTransitionException
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ChangeItemStatusUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ClaimItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DeliverItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.usecase.item.MoveCustodyUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ObserveCustodyHistoryUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ReleaseItemUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeLifecycleRepository(item: FoundItem) : FoundItemLifecycleRepository {
    val items = mutableMapOf(item.id to item)
    val history = MutableStateFlow<List<CustodyEvent>>(emptyList())

    override suspend fun getById(id: ItemId) = items[id]
    override suspend fun updateStatus(id: ItemId, status: ItemStatus) {
        items[id] = items.getValue(id).copy(status = status)
    }
    override suspend fun moveCustody(event: CustodyEvent) {
        history.value = listOf(event) + history.value
        items[event.foundItemId] = items.getValue(event.foundItemId).copy(custodyPlace = event.place, custodyDetail = event.detail)
    }
    override fun observeCustodyHistory(id: ItemId) = history.map { events -> events.filter { it.foundItemId == id } }
}

private class FakeAudit : AuditRecorder {
    val entries = mutableListOf<Triple<AuditAction, String, String>>()
    override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
        entries += Triple(action, actorId, detail)
    }
}

private object DirectTransaction : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

class ItemLifecycleUseCasesTest {

    private val id = ItemId("f-test")
    private val item = FoundItem(
        id = id, title = "Termo plateado", category = Category.OTROS, subcategory = Subcategory.entries.first {
            it.category == Category.OTROS
        },
        color = "Plateado", brand = null, description = "", campus = Campus.LA_PAZ, foundZone = CampusZone.CAFETERIA,
        foundDetail = null, foundAt = FixedClock.now(), custodyPlace = CustodyPlace.OFICINA, custodyDetail = "",
        status = ItemStatus.DISPONIBLE, photoUrls = emptyList(), controlQuestions = listOf("¿Qué sticker tiene?"),
        registeredBy = "u-ana-staff", retentionUntil = FixedClock.now(), createdAt = FixedClock.now()
    )
    private val repository = FakeLifecycleRepository(item)
    private val audit = FakeAudit()

    private fun change(role: Role?) = ChangeItemStatusUseCase(
        repository, audit, DirectTransaction, GetCurrentUserUseCase(FakeSession(role?.let(::demoUser)))
    )
    private val staff = change(Role.STAFF)
    private val status get() = repository.items.getValue(id).status

    @Test
    fun fullLifecycleIsAuditedStepByStep() = runTest {
        ClaimItemUseCase(change(Role.ESTUDIANTE))(id).getOrThrow()
        assertEquals(ItemStatus.RECLAMADO, status)
        DeliverItemUseCase(staff)(id, note = "Entregado con carnet").getOrThrow()
        assertEquals(ItemStatus.ENTREGADO, status)

        assertEquals(listOf(AuditAction.CLAIM_CREATED, AuditAction.ITEM_DELIVERED), audit.entries.map { it.first })
        assertEquals("Reclamado → Entregado · Entregado con carnet", audit.entries.last().third)
    }

    @Test
    fun rejectedClaimReleasesTheItem() = runTest {
        ClaimItemUseCase(staff)(id).getOrThrow()
        ReleaseItemUseCase(staff)(id).getOrThrow()
        assertEquals(ItemStatus.DISPONIBLE, status)
        assertEquals(AuditAction.CLAIM_REJECTED, audit.entries.last().first)
    }

    @Test
    fun invalidTransitionChangesNothingAndIsNotAudited() = runTest {
        val result = DeliverItemUseCase(staff)(id)
        assertIs<InvalidTransitionException>(result.exceptionOrNull())
        assertEquals(ItemStatus.DISPONIBLE, status)
        assertTrue(audit.entries.isEmpty())
    }

    @Test
    fun finalStatesCannotChangeAgain() = runTest {
        DonateItemUseCase(staff)(id).getOrThrow()
        assertEquals(ItemStatus.DONADO, status)
        assertIs<InvalidTransitionException>(DiscardItemUseCase(staff)(id).exceptionOrNull())
        assertIs<InvalidTransitionException>(ClaimItemUseCase(staff)(id).exceptionOrNull())
        assertEquals(1, audit.entries.size)
    }

    @Test
    fun onlyStaffDeliversDiscardsOrDonates() = runTest {
        val student = change(Role.ESTUDIANTE)
        assertEquals(ItemErrors.NOT_STAFF, DiscardItemUseCase(student)(id).exceptionOrNull()?.message)
        assertEquals(ItemErrors.NO_SESSION, ClaimItemUseCase(change(null))(id).exceptionOrNull()?.message)
        assertEquals(ItemErrors.NOT_FOUND, ClaimItemUseCase(staff)(ItemId("no-existe")).exceptionOrNull()?.message)
        assertEquals(ItemStatus.DISPONIBLE, status)
    }

    private fun move(role: Role = Role.STAFF) = MoveCustodyUseCase(
        repository, audit, DirectTransaction, GetCurrentUserUseCase(FakeSession(demoUser(role))), FixedClock
    )

    @Test
    fun movingCustodyBuildsTheHistory() = runTest {
        move()(id, CustodyPlace.LOCKER, " Locker 12 ").getOrThrow()
        move()(id, CustodyPlace.BODEGA, note = "Fin de semestre").getOrThrow()

        val history = ObserveCustodyHistoryUseCase(repository)(id).first()
        assertEquals(listOf(CustodyPlace.BODEGA, CustodyPlace.LOCKER), history.map { it.place })
        assertEquals("Locker 12", history.last().detail)
        assertEquals(CustodyPlace.BODEGA, repository.items.getValue(id).custodyPlace)
        assertEquals("Locker → Bodega", audit.entries.last().third)
        assertTrue(audit.entries.all { it.first == AuditAction.CUSTODY_CHANGED })
    }

    @Test
    fun invalidCustodyMovesAreRejected() = runTest {
        assertEquals(ItemErrors.SAME_PLACE, move()(id, CustodyPlace.OFICINA).exceptionOrNull()?.message)
        assertEquals(ItemErrors.NOT_STAFF, move(Role.ESTUDIANTE)(id, CustodyPlace.LOCKER).exceptionOrNull()?.message)

        DiscardItemUseCase(staff)(id).getOrThrow()
        assertEquals(ItemErrors.NOT_IN_CUSTODY, move()(id, CustodyPlace.LOCKER).exceptionOrNull()?.message)
        assertTrue(repository.history.value.isEmpty())
    }
}
