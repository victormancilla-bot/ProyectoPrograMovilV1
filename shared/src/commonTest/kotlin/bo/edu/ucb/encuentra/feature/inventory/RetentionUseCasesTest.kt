package bo.edu.ucb.encuentra.feature.inventory

import bo.edu.ucb.encuentra.core.InMemoryNotifications
import bo.edu.ucb.encuentra.core.RecordingNotifier
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.usecase.item.ChangeItemStatusUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.Audit
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionAction
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionErrors
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.RetentionRepository
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.CheckRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveRetentionUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.RETENTION_DEEP_LINK
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ResolveExpiredItemUseCase
import bo.edu.ucb.encuentra.feature.matching.foundItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** Objeto encontrado que vence [daysFromNow] días después de FixedClock (negativo = ya venció). */
internal fun itemExpiring(id: String, daysFromNow: Int, status: ItemStatus = ItemStatus.DISPONIBLE): FoundItem {
    val until = FixedClock.now() + daysFromNow.days
    return foundItem(id = id, foundAt = until - 90.days, status = status)
}

/** found_items + users en memoria; también hace de repositorio de H05 para Donar/Descartar. */
internal class InMemoryRetention(items: List<FoundItem>, private val staff: List<String> = listOf("u-ana-staff", "u-admin")) :
    RetentionRepository, FoundItemLifecycleRepository {
    val items = MutableStateFlow(items)

    override fun observeAvailable() = items.map { list -> list.filter { it.status == ItemStatus.DISPONIBLE } }
    override suspend fun availableItems() = items.value.filter { it.status == ItemStatus.DISPONIBLE }
    override suspend fun findItem(id: String) = items.value.firstOrNull { it.id.value == id }
    override suspend fun staffIds() = staff

    override suspend fun getById(id: ItemId) = findItem(id.value)
    override suspend fun updateStatus(id: ItemId, status: ItemStatus) {
        items.value = items.value.map { if (it.id == id) it.copy(status = status) else it }
    }
    override suspend fun moveCustody(event: CustodyEvent) = Unit
    override fun observeCustodyHistory(id: ItemId) = flowOf(emptyList<CustodyEvent>())
}

class RetentionUseCasesTest {

    private val staff = demoUser(Role.STAFF)
    private val session = FakeSession(staff)
    private val getCurrentUser = GetCurrentUserUseCase(session)
    private val repository = InMemoryRetention(
        listOf(
            itemExpiring("fi-paraguas", daysFromNow = -10),
            itemExpiring("fi-chompa", daysFromNow = -1),
            itemExpiring("fi-termo", daysFromNow = 3),
            itemExpiring("fi-libro", daysFromNow = 40),
            itemExpiring("fi-entregado", daysFromNow = -30, status = ItemStatus.ENTREGADO)
        )
    )
    private val notifications = InMemoryNotifications()
    private val notifier = RecordingNotifier()
    private val center = NotificationCenter(notifications, notifier, getCurrentUser)
    private val audit = Audit()
    private val change = ChangeItemStatusUseCase(repository, audit, Direct, getCurrentUser)
    private val resolve = ResolveExpiredItemUseCase(repository, DonateItemUseCase(change), DiscardItemUseCase(change), getCurrentUser, FixedClock)

    @Test
    fun overviewSplitsExpiredAndExpiringOldestFirst() = runTest {
        val overview = ObserveRetentionUseCase(repository, getCurrentUser, FixedClock)().first()
        assertEquals(listOf("fi-paraguas", "fi-chompa"), overview.expired.map { it.id.value })
        assertEquals(listOf("fi-termo"), overview.expiring.map { it.id.value })
    }

    @Test
    fun studentsDoNotSeeTheRetentionList() = runTest {
        session.setCurrentUser(demoUser(Role.ESTUDIANTE))
        assertTrue(ObserveRetentionUseCase(repository, getCurrentUser, FixedClock)().first().isEmpty)
    }

    @Test
    fun dailyCheckNotifiesEveryStaffMemberOncePerDay() = runTest {
        val check = CheckRetentionUseCase(repository, center, FixedClock, BoliviaTime)

        val result = check()
        check() // el mismo día: actualiza, no duplica

        assertEquals(2, result.expired)
        assertEquals(1, result.expiring)
        assertEquals(2, result.notifiedStaff)
        val saved = notifications.saved.values.sortedBy { it.userId }
        assertEquals(listOf("u-admin", "u-ana-staff"), saved.map { it.userId })
        assertTrue(saved.all { it.type == NotificationType.RETENTION && it.deepLink == RETENTION_DEEP_LINK })
        assertEquals("2 objetos vencidos", saved.first().title)
        assertEquals("En custodia hay 2 vencidos y 1 por vencer. Decide si se donan o se descartan.", saved.first().body)
        assertEquals("n-retention-u-admin-2026-11-02", saved.first().id.value)
        // El staff con sesión lo ve en el sistema; el admin, cuando entre.
        assertTrue(notifier.shown.all { it.userId == staff.id })
    }

    @Test
    fun nothingToNotifyWhenNothingExpires() = runTest {
        val clean = InMemoryRetention(listOf(itemExpiring("fi-libro", daysFromNow = 40)))
        val result = CheckRetentionUseCase(clean, center, FixedClock, BoliviaTime)()
        assertEquals(0, result.notifiedStaff)
        assertTrue(notifications.saved.isEmpty())
    }

    @Test
    fun messageForOnlyExpiringItems() {
        val (title, body) = CheckRetentionUseCase.messageFor(expired = 0, expiring = 1)
        assertEquals("1 objeto por vencer", title)
        assertEquals("En custodia hay 1 por vencer. Revisa si alguien los reclama a tiempo.", body)
    }

    @Test
    fun donateAndDiscardChangeTheStatusAndAreAudited() = runTest {
        resolve("fi-paraguas", RetentionAction.DONATE, "Cáritas").getOrThrow()
        resolve("fi-chompa", RetentionAction.DISCARD).getOrThrow()

        assertEquals(ItemStatus.DONADO, repository.findItem("fi-paraguas")?.status)
        assertEquals(ItemStatus.DESCARTADO, repository.findItem("fi-chompa")?.status)
        assertEquals(listOf(AuditAction.ITEM_DONATED, AuditAction.ITEM_DISCARDED), audit.entries.map { it.first })
        assertEquals("Disponible → Donado · Retención vencida: Cáritas", audit.entries.first().second)
    }

    @Test
    fun onlyExpiredItemsAndOnlyStaff() = runTest {
        assertEquals(RetentionErrors.NOT_EXPIRED, resolve("fi-termo", RetentionAction.DONATE).exceptionOrNull()?.message)
        assertEquals(RetentionErrors.NOT_FOUND, resolve("fi-x", RetentionAction.DONATE).exceptionOrNull()?.message)

        session.setCurrentUser(demoUser(Role.ESTUDIANTE))
        assertEquals(RetentionErrors.NOT_STAFF, resolve("fi-paraguas", RetentionAction.DONATE).exceptionOrNull()?.message)
        assertEquals(ItemStatus.DISPONIBLE, repository.findItem("fi-paraguas")?.status)
        assertTrue(audit.entries.isEmpty())
    }
}
