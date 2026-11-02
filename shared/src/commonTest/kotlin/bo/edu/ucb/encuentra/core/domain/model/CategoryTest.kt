package bo.edu.ucb.encuentra.core.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class CategoryTest {

    @Test
    fun everyCategoryHasSubcategories() {
        Category.entries.forEach { category ->
            assertTrue(category.subcategories.isNotEmpty(), "$category no tiene subcategorías")
        }
    }

    @Test
    fun subcategoriesBelongToTheirCategory() {
        assertTrue(Subcategory.LAPTOP in Category.ELECTRONICA.subcategories)
        assertTrue(Subcategory.LAPTOP !in Category.LLAVES.subcategories)
    }
}
