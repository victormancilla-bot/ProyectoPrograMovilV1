package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimStateMachine
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimTransition
import bo.edu.ucb.encuentra.feature.claims.domain.model.InvalidClaimTransitionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClaimStateMachineTest {

    @Test
    fun happyPathGoesFromPendingToDelivered() {
        var status = ClaimStatus.PENDIENTE
        status = ClaimStateMachine.apply(status, ClaimTransition.SUBMIT_ANSWERS)
        assertEquals(ClaimStatus.EN_VERIFICACION, status)
        status = ClaimStateMachine.apply(status, ClaimTransition.APPROVE)
        assertEquals(ClaimStatus.APROBADO, status)
        status = ClaimStateMachine.apply(status, ClaimTransition.DELIVER)
        assertEquals(ClaimStatus.ENTREGADO, status)
    }

    @Test
    fun canBeRejectedWhilePendingOrInReview() {
        assertEquals(ClaimStatus.RECHAZADO, ClaimStateMachine.apply(ClaimStatus.PENDIENTE, ClaimTransition.REJECT))
        assertEquals(ClaimStatus.RECHAZADO, ClaimStateMachine.apply(ClaimStatus.EN_VERIFICACION, ClaimTransition.REJECT))
    }

    @Test
    fun invalidTransitionsAreRejected() {
        // No se aprueba sin responder las preguntas, ni se entrega sin aprobar.
        assertFailsWith<InvalidClaimTransitionException> { ClaimStateMachine.apply(ClaimStatus.PENDIENTE, ClaimTransition.APPROVE) }
        assertFailsWith<InvalidClaimTransitionException> { ClaimStateMachine.apply(ClaimStatus.EN_VERIFICACION, ClaimTransition.DELIVER) }
        assertFailsWith<InvalidClaimTransitionException> { ClaimStateMachine.apply(ClaimStatus.APROBADO, ClaimTransition.REJECT) }
        val error = assertFailsWith<InvalidClaimTransitionException> {
            ClaimStateMachine.apply(ClaimStatus.EN_VERIFICACION, ClaimTransition.SUBMIT_ANSWERS)
        }
        assertEquals("No se puede responder un reclamo en estado \"En verificación\".", error.message)
    }

    @Test
    fun rejectedAndDeliveredAreFinal() {
        for (status in listOf(ClaimStatus.RECHAZADO, ClaimStatus.ENTREGADO)) {
            assertTrue(ClaimStateMachine.isFinal(status))
            assertFalse(ClaimStateMachine.isOpen(status))
            ClaimTransition.entries.forEach { transition ->
                assertFailsWith<InvalidClaimTransitionException> { ClaimStateMachine.apply(status, transition) }
            }
        }
        assertTrue(listOf(ClaimStatus.PENDIENTE, ClaimStatus.EN_VERIFICACION, ClaimStatus.APROBADO).all(ClaimStateMachine::isOpen))
    }
}
