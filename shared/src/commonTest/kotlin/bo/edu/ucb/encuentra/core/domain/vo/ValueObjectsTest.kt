package bo.edu.ucb.encuentra.core.domain.vo

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ValueObjectsTest {

    @Test
    fun emailAcceptsOnlyInstitutionalAccounts() {
        assertTrue(Email("mateo@ucb.edu.bo").isInstitutional())
        assertTrue(Email("  Ana.Gomez@UCB.EDU.BO ").isInstitutional())
        assertFalse(Email("mateo@gmail.com").isInstitutional())
        assertFalse(Email("mateo@ucb.edu.bo.com").isInstitutional())
        assertFalse(Email("@ucb.edu.bo").isInstitutional())
    }

    @Test
    fun blankValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { Email(" ") }
        assertFailsWith<IllegalArgumentException> { Password("") }
        assertFailsWith<IllegalArgumentException> { ItemId("") }
    }

    @Test
    fun passwordNeedsMinimumLength() {
        assertTrue(Password("123456").isValid())
        assertFalse(Password("12345").isValid())
    }

    @Test
    fun generatedItemIdsAreUnique() {
        assertNotEquals(ItemId.generate(), ItemId.generate())
    }
}
