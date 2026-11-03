package bo.edu.ucb.encuentra.feature.audit

import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ChangeItemStatusUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.MoveCustodyUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.claims.ItemsOf
import bo.edu.ucb.encuentra.feature.claims.SeedClaimStore
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.claims.luis
import bo.edu.ucb.encuentra.feature.claims.staff
import bo.edu.ucb.encuentra.feature.delivery.DeliveryNotifier
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ConfirmDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.PhysicalChecks
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ScheduleDeliveryUseCase
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.inventory.InMemoryRetention
import bo.edu.ucb.encuentra.feature.inventory.itemExpiring
import bo.edu.ucb.encuentra.feature.myreports.FakeMyReportsRepository
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.CloseReportUseCase
import bo.edu.ucb.encuentra.feature.myreports.mateo
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ChangeUserRoleUseCase
import bo.edu.ucb.encuentra.feature.profile.seedUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

/** Un registro completo: qué pasó, QUIÉN lo hizo y SOBRE QUÉ (lo que se necesita ante un reclamo falso). */
private data class Entry(val action: AuditAction, val actorId: String, val targetId: String)

private class Trail : AuditRecorder {
    val entries = mutableListOf<Entry>()
    override suspend fun record(action: AuditAction, actorId: String, targetId: String, detail: String) {
        entries += Entry(action, actorId, targetId)
    }
}

/**
 * S05 · Cada use case sensible escribe su evento, con el actor correcto. Si alguien agrega un use case que cambia
 * estados sin auditar, este test lo delata.
 */
class AuditTrailTest {

    private val trail = Trail()
    private val session = FakeSession(mateo)
    private val user = GetCurrentUserUseCase(session)

    private suspend fun <T> asUser(who: UserModel, block: suspend () -> T): T {
        session.setCurrentUser(who)
        return block()
    }

    @Test
    fun claimLifecycleRecordsWhoClaimedAnsweredApprovedScheduledAndDelivered() = runTest {
        val store = SeedClaimStore()
        val change = ChangeClaimStatusUseCase(store, ItemsOf(store), trail, Direct, user, FixedClock)
        val create = CreateClaimUseCase(store, ItemsOf(store), trail, Direct, FixedClock)
        val schedule = ScheduleDeliveryUseCase(store, ItemsOf(store), trail, Direct, user, FixedClock, BoliviaTime, DeliveryNotifier())

        val claim = store.claim(create.requestClaim(ItemId("m-llavero"), mateo.id))
        SubmitClaimAnswersUseCase(change)(claim.id, listOf("Un colgante de la UCB")).getOrThrow()
        asUser(staff) { ApproveClaimUseCase(change)(claim.id, "Describió el colgante").getOrThrow() }
        asUser(staff) { schedule(claim.id, FixedClock.now() + 1.days).getOrThrow() }
        asUser(staff) { ConfirmDeliveryUseCase(DeliverClaimUseCase(change))(claim.id, PhysicalChecks(identityVerified = true, itemReceived = true)).getOrThrow() }

        val id = claim.id.value
        assertEquals(
            listOf(
                Entry(AuditAction.CLAIM_CREATED, mateo.id, id),
                Entry(AuditAction.CLAIM_ANSWERED, mateo.id, id),
                Entry(AuditAction.CLAIM_APPROVED, staff.id, id),
                Entry(AuditAction.DELIVERY_SCHEDULED, staff.id, id),
                Entry(AuditAction.ITEM_DELIVERED, staff.id, id)
            ),
            trail.entries
        )
    }

    @Test
    fun rejectionAndWrongAnswersAreRecorded() = runTest {
        val store = SeedClaimStore()
        val change = ChangeClaimStatusUseCase(store, ItemsOf(store), trail, Direct, user, FixedClock)

        // Luis responde mal su reclamo #1025 (cada intento queda registrado) y el staff rechaza el de Carlos.
        asUser(luis) { SubmitClaimAnswersUseCase(change)(ItemId("c-1025"), listOf("Otro nombre", "fx-82")).getOrThrow() }
        asUser(staff) { RejectClaimUseCase(change)(ItemId("c-1026"), "No coincide la descripción").getOrThrow() }

        assertEquals(
            listOf(Entry(AuditAction.CLAIM_ANSWERED, luis.id, "c-1025"), Entry(AuditAction.CLAIM_REJECTED, staff.id, "c-1026")),
            trail.entries
        )
    }

    @Test
    fun retentionCustodyRolesAndReportsAreRecorded() = runTest {
        val items = InMemoryRetention(listOf(itemExpiring("fi-paraguas", -5), itemExpiring("fi-chamarra", -5), itemExpiring("fi-termo", 30)))
        val itemChange = ChangeItemStatusUseCase(items, trail, Direct, user)
        val admin = seedUser("u-admin")

        asUser(staff) {
            DonateItemUseCase(itemChange)(ItemId("fi-paraguas")).getOrThrow()
            DiscardItemUseCase(itemChange)(ItemId("fi-chamarra")).getOrThrow()
            MoveCustodyUseCase(items, trail, Direct, user, FixedClock)(ItemId("fi-termo"), CustodyPlace.LOCKER, "Locker 3").getOrThrow()
        }
        asUser(admin) { ChangeUserRoleUseCase(InMemoryUsers(), trail, Direct, user)("u-ana-gomez", Role.STAFF).getOrThrow() }
        asUser(mateo) { CloseReportUseCase(FakeMyReportsRepository(), user, trail)(ItemId("lr-laptop"), CloseReason.FOUND_IT).getOrThrow() }

        assertEquals(
            listOf(
                Entry(AuditAction.ITEM_DONATED, staff.id, "fi-paraguas"),
                Entry(AuditAction.ITEM_DISCARDED, staff.id, "fi-chamarra"),
                Entry(AuditAction.CUSTODY_CHANGED, staff.id, "fi-termo"),
                Entry(AuditAction.ROLE_CHANGED, admin.id, "u-ana-gomez"),
                Entry(AuditAction.REPORT_CLOSED, mateo.id, "lr-laptop")
            ),
            trail.entries
        )
    }
}
