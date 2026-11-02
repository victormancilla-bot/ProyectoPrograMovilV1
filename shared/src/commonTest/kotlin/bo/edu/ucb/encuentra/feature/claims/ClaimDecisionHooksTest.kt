package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.repository.ClaimNotifier
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.data.RoomClaimNotifier
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.myreports.mateo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Guarda a quién se avisó y con qué estado (hook de N01). */
private class RecordingNotifier : ClaimNotifier {
    val sent = mutableListOf<Triple<String, Int, ClaimStatus>>()
    override suspend fun onClaimUpdated(claim: Claim, itemTitle: String) {
        sent += Triple(claim.claimantId, claim.number, claim.status)
    }
}

/** V03: motivo obligatorio y hooks de auditoría (S05) y notificación (N01) en cada decisión. */
class ClaimDecisionHooksTest {

    private val store = SeedClaimStore()
    private val audit = Audit()
    private val notifier = RecordingNotifier()
    private val session = FakeSession(staff)
    private val change = ChangeClaimStatusUseCase(
        store, ItemsOf(store), audit, Direct, GetCurrentUserUseCase(session), FixedClock, notifier
    )
    private val approve = ApproveClaimUseCase(change)
    private val reject = RejectClaimUseCase(change)

    @Test
    fun approvingOrRejectingNeedsAReason() = runTest {
        assertEquals(ClaimErrors.REASON_REQUIRED, approve(ItemId("c-1024"), "  ").exceptionOrNull()?.message)
        assertEquals(ClaimErrors.REASON_REQUIRED, reject(ItemId("c-1024"), "").exceptionOrNull()?.message)
        assertEquals(ClaimStatus.EN_VERIFICACION, store.claim(1024).status)
        assertTrue(audit.entries.isEmpty() && notifier.sent.isEmpty())
    }

    @Test
    fun aDecisionIsAuditedWithItsReasonAndNotifiedToTheClaimant() = runTest {
        approve(ItemId("c-1024"), "Describió el interior café").getOrThrow()

        assertEquals("Reclamo #1024 · En verificación → Aprobado · Describió el interior café", audit.entries.last().second)
        assertEquals(listOf(Triple("u-ana-gomez", 1024, ClaimStatus.APROBADO)), notifier.sent)

        reject(ItemId("c-1025"), "No es el dueño").getOrThrow()
        assertEquals(Triple("u-luis-mendoza", 1025, ClaimStatus.RECHAZADO), notifier.sent.last())
    }

    @Test
    fun deliveringNotifiesTheOwnerAndTheOtherClaimants() = runTest {
        session.setCurrentUser(mateo)
        val create = CreateClaimUseCase(store, ItemsOf(store), audit, Direct, FixedClock)
        val mateoClaim = store.claim(create.requestClaim(ItemId("m-calculadora"), mateo.id))
        SubmitClaimAnswersUseCase(change)(mateoClaim.id, listOf("Mateo Quispe", "fx-991")).getOrThrow()

        session.setCurrentUser(staff)
        approve(mateoClaim.id, "Nombre y modelo correctos").getOrThrow()
        DeliverClaimUseCase(change)(mateoClaim.id).getOrThrow()

        // Mateo: aprobado y entregado. Luis (#1025, abierto): rechazado porque el objeto ya se entregó.
        assertEquals(
            setOf(
                Triple("u-mateo", 1027, ClaimStatus.APROBADO),
                Triple("u-mateo", 1027, ClaimStatus.ENTREGADO),
                Triple("u-luis-mendoza", 1025, ClaimStatus.RECHAZADO)
            ),
            notifier.sent.toSet()
        )
    }

    @Test
    fun notificationTextsDependOnTheNewStatus() {
        val claim = SeedData.claims.first()
        val approved = RoomClaimNotifier.messageFor(claim.copy(status = ClaimStatus.APROBADO), "Billetera Negra de Cuero")
        assertEquals("Reclamo #1024 aprobado", approved?.first)

        val blocked = RoomClaimNotifier.messageFor(
            claim.copy(status = ClaimStatus.RECHAZADO, failedAttempts = Claim.MAX_ANSWER_ATTEMPTS), "Billetera"
        )
        assertEquals("Se agotaron los intentos para responder las preguntas de \"Billetera\".", blocked?.second)

        // Responder no avisa al estudiante (él mismo lo hizo).
        assertNull(RoomClaimNotifier.messageFor(claim.copy(status = ClaimStatus.EN_VERIFICACION), "Billetera"))
    }
}
