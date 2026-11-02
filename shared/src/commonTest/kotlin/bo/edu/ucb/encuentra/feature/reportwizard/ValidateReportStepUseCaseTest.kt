package bo.edu.ucb.encuentra.feature.reportwizard

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateReportStepUseCaseTest {

    private val validate = ValidateReportStepUseCase()
    private val step1 = ValidateReportStepUseCase.STEP_CATEGORY
    private val step2 = ValidateReportStepUseCase.STEP_ATTRIBUTES

    private val validAttributes = ReportDraft(
        category = Category.ELECTRONICA,
        subcategory = Subcategory.LAPTOP,
        title = "Laptop Dell XPS",
        color = "Plateado"
    )

    @Test
    fun categoryStepNeedsCategoryAndSubcategory() {
        assertEquals(
            setOf(ReportFieldError.CATEGORY_REQUIRED, ReportFieldError.SUBCATEGORY_REQUIRED),
            validate(step1, ReportDraft())
        )
        assertEquals(setOf(ReportFieldError.SUBCATEGORY_REQUIRED), validate(step1, ReportDraft(category = Category.LLAVES)))
        assertTrue(validate(step1, ReportDraft(Category.LLAVES, Subcategory.LLAVERO)).isEmpty())
    }

    @Test
    fun subcategoryFromAnotherCategoryIsInvalid() {
        val mismatch = ReportDraft(category = Category.LLAVES, subcategory = Subcategory.LAPTOP)
        assertEquals(setOf(ReportFieldError.SUBCATEGORY_REQUIRED), validate(step1, mismatch))
    }

    @Test
    fun attributesStepNeedsTitleAndColorButBrandAndFeaturesAreOptional() {
        assertTrue(validate(step2, validAttributes).isEmpty())
        assertEquals(
            setOf(ReportFieldError.TITLE_TOO_SHORT, ReportFieldError.COLOR_REQUIRED),
            validate(step2, validAttributes.copy(title = " ab ", color = null))
        )
    }

    @Test
    fun attributesHaveMaximumLengths() {
        val tooLong = validAttributes.copy(
            title = "x".repeat(ValidateReportStepUseCase.TITLE_MAX + 1),
            brand = "x".repeat(ValidateReportStepUseCase.BRAND_MAX + 1),
            features = "x".repeat(ValidateReportStepUseCase.FEATURES_MAX + 1)
        )
        assertEquals(
            setOf(ReportFieldError.TITLE_TOO_LONG, ReportFieldError.BRAND_TOO_LONG, ReportFieldError.FEATURES_TOO_LONG),
            validate(step2, tooLong)
        )
    }

    @Test
    fun stepsThreeAndFourAreValidatedByOtherCards() {
        assertTrue(validate(3, ReportDraft()).isEmpty())
        assertTrue(validate(4, ReportDraft()).isEmpty())
    }
}
