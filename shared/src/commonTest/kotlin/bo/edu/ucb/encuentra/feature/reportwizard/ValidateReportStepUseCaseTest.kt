package bo.edu.ucb.encuentra.feature.reportwizard

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.TimeSlot
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ValidateReportStepUseCaseTest {

    private val validate = ValidateReportStepUseCase(FixedClock, BoliviaTime)
    private val step1 = ValidateReportStepUseCase.STEP_CATEGORY
    private val step2 = ValidateReportStepUseCase.STEP_ATTRIBUTES
    private val step3 = ValidateReportStepUseCase.STEP_WHERE_WHEN

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

    // "Hoy" es el 02/11/2026 (FixedClock).
    private val validWhereWhen = validAttributes.copy(
        zone = CampusZone.BIBLIOTECA_CENTRAL,
        lostFrom = LocalDate(2026, 10, 30),
        lostTo = LocalDate(2026, 11, 2),
        timeSlot = TimeSlot.MANANA
    )

    @Test
    fun whereWhenStepNeedsZoneDatesAndTimeSlot() {
        assertTrue(validate(step3, validWhereWhen).isEmpty())
        assertEquals(
            setOf(ReportFieldError.ZONE_REQUIRED, ReportFieldError.DATE_REQUIRED, ReportFieldError.TIME_SLOT_REQUIRED),
            validate(step3, ReportDraft())
        )
    }

    @Test
    fun dateRangeMustBeOrderedInThePastAndAtMostThirtyDays() {
        assertEquals(
            setOf(ReportFieldError.DATE_RANGE_INVERTED),
            validate(step3, validWhereWhen.copy(lostFrom = LocalDate(2026, 11, 2), lostTo = LocalDate(2026, 11, 1)))
        )
        assertEquals(
            setOf(ReportFieldError.DATE_IN_FUTURE),
            validate(step3, validWhereWhen.copy(lostTo = LocalDate(2026, 11, 3)))
        )
        assertEquals(
            setOf(ReportFieldError.DATE_RANGE_TOO_WIDE),
            validate(step3, validWhereWhen.copy(lostFrom = LocalDate(2026, 10, 3)))
        )
        // Un solo día es un rango válido.
        assertTrue(validate(step3, validWhereWhen.copy(lostFrom = LocalDate(2026, 11, 2))).isEmpty())
    }

    @Test
    fun phoneIsOptionalButMustBeBolivian() {
        listOf("", "71234567", "+591 71234567", "591-7123-4567", "22441122").forEach {
            assertTrue(validate(step3, validWhereWhen.copy(contactPhone = it)).isEmpty(), it)
        }
        listOf("1234", "81234567", "+54 1123456789", "7123456a").forEach {
            assertEquals(setOf(ReportFieldError.PHONE_INVALID), validate(step3, validWhereWhen.copy(contactPhone = it)), it)
        }
    }

    @Test
    fun lostRangeUsesTheTimeSlotOnTheFirstAndLastDay() {
        assertEquals(
            Instant.parse("2026-10-30T06:00:00-04:00") to Instant.parse("2026-11-02T12:00:00-04:00"),
            validate.lostRange(validWhereWhen)
        )
    }

    @Test
    fun upToChecksEveryPreviousStepAndStepFourAddsNothing() {
        assertTrue(validate.upTo(step3, validWhereWhen).isEmpty())
        assertTrue(validate(4, ReportDraft()).isEmpty())
        assertTrue(ReportFieldError.CATEGORY_REQUIRED in validate.upTo(step3, validWhereWhen.copy(category = null)))
    }
}
