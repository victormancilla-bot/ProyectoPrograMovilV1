package bo.edu.ucb.encuentra.feature.delivery

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.ClaimNotifier
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.Audit
import bo.edu.ucb.encuentra.feature.claims.Direct
import bo.edu.ucb.encuentra.feature.claims.ItemsOf
import bo.edu.ucb.encuentra.feature.claims.SeedClaimStore
import bo.edu.ucb.encuentra.feature.claims.staff
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ConfirmDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.DeliveryErrors
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.PhysicalChecks
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ScheduleDeliveryUseCase
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Qué se le avisó al estudiante (hook de N01). */
internal class DeliveryNotifier : ClaimNotifier {
    val updates = mutableListOf<Pair<Int, ClaimStatus>>()
    val scheduled = mutableListOf<Claim>()
    override suspend fun onClaimUpdated(claim: Claim, itemTitle: String) {
        updates += claim.number to claim.status
    }
    override suspend fun onDeliveryScheduled(claim: Claim, itemTitle: String) {
        scheduled += claim
    }
}

class DeliveryUseCasesTest {

    private val store = SeedClaimStore()
    private val audit = Audit()
    private val notifier = DeliveryNotifier()
    private val session = FakeSession(mateo)
    private val user = GetCurrentUserUseCase(session)
    private val change = ChangeClaimStatusUseCase(store, ItemsOf(store), audit, Direct, user, FixedClock, notifier)
    private val schedule = ScheduleDeliveryUseCase(store, ItemsOf(store), audit, Direct, user, FixedClock, BoliviaTime, notifier)
    private val confirm = ConfirmDeliveryUseCase(DeliverClaimUseCase(change))

    /** Mateo reclama el llavero, responde bien y el staff aprueba: queda listo para entrega. */
    private suspend fun approvedLlavero(): Claim {
        val number = CreateClaimUseCase(store, ItemsOf(store), audit, Direct, FixedClock).requestClaim(ItemId("m-llavero"), mateo.id)
        val claim = store.claim(number)
        SubmitClaimAnswersUseCase(change)(claim.id, listOf("Escudo de la UCB")).getOrThrow()
        session.setCurrentUser(staff)
        ApproveClaimUseCase(change)(claim.id, "Describió el colgante").getOrThrow()
        return store.claim(number)
    }

    @Test
    fun scheduleSavesTheDateAuditsAndNotifies() = runTest {
        val claim = approvedLlavero()
        val at = FixedClock.now() + 2.days + 2.hours

        val scheduled = schedule(claim.id, at).getOrThrow()

        assertEquals(at, scheduled.scheduledDeliveryAt)
        assertEquals(at, store.claim(claim.number).scheduledDeliveryAt)
        assertEquals(AuditAction.DELIVERY_SCHEDULED to "Reclamo #1027 · entrega programada para 2026-11-04 14:00", audit.entries.last())
        assertEquals(listOf(at), notifier.scheduled.map { it.scheduledDeliveryAt })
    }

    @Test
    fun onlyStaffSchedulesApprovedClaimsInTheFuture() = runTest {
        val claim = approvedLlavero()

        assertEquals(DeliveryErrors.PAST_DATE, schedule(claim.id, FixedClock.now() - 1.hours).exceptionOrNull()?.message)
        assertEquals(DeliveryErrors.NOT_APPROVED, schedule(ItemId("c-1025"), FixedClock.now() + 1.days).exceptionOrNull()?.message)
        session.setCurrentUser(mateo)
        assertEquals(ItemErrors.NOT_STAFF, schedule(claim.id, FixedClock.now() + 1.days).exceptionOrNull()?.message)
        assertTrue(notifier.scheduled.isEmpty())
    }

    @Test
    fun deliveryNeedsTheWholePhysicalChecklist() = runTest {
        val claim = approvedLlavero()

        for (checks in listOf(PhysicalChecks(), PhysicalChecks(identityVerified = true), PhysicalChecks(itemReceived = true))) {
            assertEquals(DeliveryErrors.CHECKS_REQUIRED, confirm(claim.id, checks).exceptionOrNull()?.message)
        }
        assertEquals(ClaimStatus.APROBADO, store.claim(claim.number).status)
        assertEquals(ItemStatus.RECLAMADO, store.item("fi-llavero"))
    }

    @Test
    fun confirmingDeliversAuditsAndNotifies() = runTest {
        val claim = approvedLlavero()

        val delivered = confirm(claim.id, PhysicalChecks(identityVerified = true, itemReceived = true)).getOrThrow()

        assertEquals(ClaimStatus.ENTREGADO, delivered.status)
        assertEquals(staff.id, delivered.deliveredBy)
        assertEquals(FixedClock.now(), delivered.deliveredAt)
        assertEquals(ItemStatus.ENTREGADO, store.item("fi-llavero"))
        assertEquals(ReportStatus.RECUPERADO, store.report("lr-calculadora"))
        assertEquals(
            AuditAction.ITEM_DELIVERED to "Reclamo #1027 · Aprobado → Entregado · ${ConfirmDeliveryUseCase.NOTE}",
            audit.entries.last()
        )
        assertEquals(1027 to ClaimStatus.ENTREGADO, notifier.updates.last())
    }
}
