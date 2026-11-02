package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.domain.model.InvalidTransitionException
import bo.edu.ucb.encuentra.core.domain.model.ItemStateMachine
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemStateMachineTest {

    /** Todas las transiciones válidas; cualquier otra combinación debe fallar. */
    private val valid = setOf(
        ItemStatus.DISPONIBLE to ItemTransition.CLAIM,
        ItemStatus.DISPONIBLE to ItemTransition.DISCARD,
        ItemStatus.DISPONIBLE to ItemTransition.DONATE,
        ItemStatus.RECLAMADO to ItemTransition.DELIVER,
        ItemStatus.RECLAMADO to ItemTransition.RELEASE
    )

    @Test
    fun happyPathGoesFromAvailableToDelivered() {
        val claimed = ItemStateMachine.apply(ItemStatus.DISPONIBLE, ItemTransition.CLAIM)
        assertEquals(ItemStatus.RECLAMADO, claimed)
        assertEquals(ItemStatus.ENTREGADO, ItemStateMachine.apply(claimed, ItemTransition.DELIVER))
    }

    @Test
    fun rejectedClaimReturnsTheItemToAvailable() {
        assertEquals(ItemStatus.DISPONIBLE, ItemStateMachine.apply(ItemStatus.RECLAMADO, ItemTransition.RELEASE))
    }

    @Test
    fun everyCombinationOutsideTheTableIsInvalid() {
        for (status in ItemStatus.entries) for (transition in ItemTransition.entries) {
            val pair = status to transition
            assertEquals(pair in valid, ItemStateMachine.canApply(status, transition), "$pair")
            if (pair !in valid) {
                val error = assertFailsWith<InvalidTransitionException> { ItemStateMachine.apply(status, transition) }
                assertEquals(status, error.from)
            }
        }
    }

    @Test
    fun cannotDeliverWithoutAClaimNorDiscardAClaimedItem() {
        assertFailsWith<InvalidTransitionException> { ItemStateMachine.apply(ItemStatus.DISPONIBLE, ItemTransition.DELIVER) }
        val error = assertFailsWith<InvalidTransitionException> {
            ItemStateMachine.apply(ItemStatus.RECLAMADO, ItemTransition.DISCARD)
        }
        assertEquals("No se puede descartar un objeto en estado \"Reclamado\"", error.message)
    }

    @Test
    fun finalStatesHaveNoWayOutAndLeaveCustody() {
        listOf(ItemStatus.ENTREGADO, ItemStatus.DESCARTADO, ItemStatus.DONADO).forEach {
            assertTrue(ItemStateMachine.isFinal(it))
            assertFalse(ItemStateMachine.isInCustody(it))
        }
        assertTrue(ItemStateMachine.isInCustody(ItemStatus.DISPONIBLE))
        assertTrue(ItemStateMachine.isInCustody(ItemStatus.RECLAMADO))
    }
}
