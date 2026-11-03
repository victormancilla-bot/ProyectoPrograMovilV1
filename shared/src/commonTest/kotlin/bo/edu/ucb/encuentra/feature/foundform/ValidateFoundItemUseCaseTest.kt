package bo.edu.ucb.encuentra.feature.foundform

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.FoundFieldError
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Reloj fijo: "ahora" es el 02/11/2026 12:00 en Bolivia (UTC-4). */
internal object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-11-02T12:00:00-04:00")
}

internal val BoliviaTime = TimeZone.of("America/La_Paz")

internal val validFoundDraft = FoundItemDraft(
    photos = listOf("file:///data/found_photos/1.jpg"),
    category = Category.ELECTRONICA,
    subcategory = Subcategory.CELULAR,
    color = "Negro",
    title = "Smartphone Negro",
    foundZone = CampusZone.BIBLIOTECA_CENTRAL,
    custodyPlace = CustodyPlace.OFICINA,
    foundDate = LocalDate(2026, 11, 2),
    foundTime = LocalTime(9, 30),
    controlQuestions = listOf("¿Qué fondo de pantalla tiene?"),
    expectedAnswers = listOf("Un perro")
)

class ValidateFoundItemUseCaseTest {

    private val validate = ValidateFoundItemUseCase(FixedClock, BoliviaTime)

    @Test
    fun completeDraftIsValidAndBrandAndFeaturesAreOptional() {
        assertTrue(validate(validFoundDraft).isEmpty())
    }

    @Test
    fun emptyDraftListsEveryRequiredField() {
        assertEquals(
            setOf(
                FoundFieldError.PHOTO_REQUIRED, FoundFieldError.CATEGORY_REQUIRED, FoundFieldError.SUBCATEGORY_REQUIRED, FoundFieldError.COLOR_REQUIRED,
                FoundFieldError.TITLE_TOO_SHORT, FoundFieldError.FOUND_ZONE_REQUIRED, FoundFieldError.CUSTODY_REQUIRED,
                FoundFieldError.FOUND_DATE_REQUIRED, FoundFieldError.QUESTION_REQUIRED
            ),
            validate(FoundItemDraft())
        )
    }

    @Test
    fun custodyLocationIsMandatory() {
        assertEquals(setOf(FoundFieldError.CUSTODY_REQUIRED), validate(validFoundDraft.copy(custodyPlace = null)))
    }

    @Test
    fun foundDateCannotBeInTheFuture() {
        val later = validFoundDraft.copy(foundTime = LocalTime(15, 0))
        assertEquals(setOf(FoundFieldError.FOUND_DATE_IN_FUTURE), validate(later))
        assertEquals(setOf(FoundFieldError.FOUND_DATE_REQUIRED), validate(validFoundDraft.copy(foundTime = null)))
    }

    @Test
    fun controlQuestionsNeedOneToThree() {
        assertEquals(setOf(FoundFieldError.QUESTION_REQUIRED), validate(validFoundDraft.copy(controlQuestions = listOf(" ", ""))))
        assertEquals(
            setOf(FoundFieldError.TOO_MANY_QUESTIONS),
            validate(validFoundDraft.copy(controlQuestions = listOf("a?", "b?", "c?", "d?"), expectedAnswers = listOf("A", "B", "C", "D")))
        )
        assertEquals(
            setOf(FoundFieldError.QUESTION_TOO_LONG),
            validate(validFoundDraft.copy(controlQuestions = listOf("x".repeat(ValidateFoundItemUseCase.QUESTION_MAX + 1))))
        )
    }

    @Test
    fun atLeastOnePhotoIsRequired() {
        assertEquals(setOf(FoundFieldError.PHOTO_REQUIRED), validate(validFoundDraft.copy(photos = emptyList())))
    }

    @Test
    fun everyQuestionNeedsItsExpectedAnswer() {
        assertEquals(setOf(FoundFieldError.ANSWER_REQUIRED), validate(validFoundDraft.copy(expectedAnswers = listOf("  "))))
        assertEquals(
            setOf(FoundFieldError.ANSWER_REQUIRED),
            validate(validFoundDraft.copy(controlQuestions = listOf("a?", "b?"), expectedAnswers = listOf("A")))
        )
        assertEquals(
            setOf(FoundFieldError.ANSWER_TOO_LONG),
            validate(validFoundDraft.copy(expectedAnswers = listOf("x".repeat(ValidateFoundItemUseCase.ANSWER_MAX + 1))))
        )
        // Una pregunta vacía no exige respuesta.
        assertEquals(emptySet(), validate(validFoundDraft.copy(controlQuestions = listOf("a?", " "), expectedAnswers = listOf("A", ""))))
    }

    @Test
    fun foundAtCombinesDateAndTimeInTheLocalZone() {
        assertEquals(Instant.parse("2026-11-02T09:30:00-04:00"), validate.foundAt(validFoundDraft))
    }
}
