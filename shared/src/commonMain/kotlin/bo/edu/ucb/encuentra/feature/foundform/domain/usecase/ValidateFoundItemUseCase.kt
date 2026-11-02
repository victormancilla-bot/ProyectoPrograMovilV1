package bo.edu.ucb.encuentra.feature.foundform.domain.usecase

import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

enum class FoundFieldError(val message: String) {
    CATEGORY_REQUIRED("Elige la categoría"),
    SUBCATEGORY_REQUIRED("Elige la subcategoría"),
    COLOR_REQUIRED("Elige el color predominante"),
    TITLE_TOO_SHORT("Escribe qué es (mínimo ${ValidateFoundItemUseCase.TITLE_MIN} letras)"),
    TITLE_TOO_LONG("Máximo ${ValidateFoundItemUseCase.TITLE_MAX} caracteres"),
    BRAND_TOO_LONG("Máximo ${ValidateFoundItemUseCase.BRAND_MAX} caracteres"),
    FEATURES_TOO_LONG("Máximo ${ValidateFoundItemUseCase.FEATURES_MAX} caracteres"),
    FOUND_ZONE_REQUIRED("Elige dónde se encontró"),
    CUSTODY_REQUIRED("Indica dónde queda guardado el objeto"),
    FOUND_DATE_REQUIRED("Indica la fecha y hora del hallazgo"),
    FOUND_DATE_IN_FUTURE("La fecha del hallazgo no puede ser futura"),
    QUESTION_REQUIRED("Agrega al menos una pregunta de control"),
    TOO_MANY_QUESTIONS("Máximo ${ValidateFoundItemUseCase.MAX_QUESTIONS} preguntas"),
    QUESTION_TOO_LONG("Cada pregunta: máximo ${ValidateFoundItemUseCase.QUESTION_MAX} caracteres")
}

/** Reglas del formulario "Registrar Objeto" (H03). El botón Registrar se habilita solo sin errores. */
class ValidateFoundItemUseCase(
    private val clock: Clock,
    private val timeZone: TimeZone
) {
    operator fun invoke(draft: FoundItemDraft): Set<FoundFieldError> = buildSet {
        if (draft.category == null) add(FoundFieldError.CATEGORY_REQUIRED)
        val sub = draft.subcategory
        if (sub == null || sub.category != draft.category) add(FoundFieldError.SUBCATEGORY_REQUIRED)
        if (draft.color.isNullOrBlank()) add(FoundFieldError.COLOR_REQUIRED)

        val title = draft.title.trim()
        if (title.length < TITLE_MIN) add(FoundFieldError.TITLE_TOO_SHORT)
        if (title.length > TITLE_MAX) add(FoundFieldError.TITLE_TOO_LONG)
        if (draft.brand.trim().length > BRAND_MAX) add(FoundFieldError.BRAND_TOO_LONG)
        if (draft.features.trim().length > FEATURES_MAX) add(FoundFieldError.FEATURES_TOO_LONG)

        if (draft.foundZone == null) add(FoundFieldError.FOUND_ZONE_REQUIRED)
        if (draft.custodyPlace == null) add(FoundFieldError.CUSTODY_REQUIRED)

        val foundAt = foundAt(draft)
        when {
            foundAt == null -> add(FoundFieldError.FOUND_DATE_REQUIRED)
            foundAt > clock.now() -> add(FoundFieldError.FOUND_DATE_IN_FUTURE)
        }

        val questions = draft.controlQuestions.map { it.trim() }.filter { it.isNotEmpty() }
        if (questions.isEmpty()) add(FoundFieldError.QUESTION_REQUIRED)
        if (draft.controlQuestions.size > MAX_QUESTIONS) add(FoundFieldError.TOO_MANY_QUESTIONS)
        if (questions.any { it.length > QUESTION_MAX }) add(FoundFieldError.QUESTION_TOO_LONG)
    }

    /** Fecha y hora del hallazgo en la zona del dispositivo, o null si falta alguna. */
    fun foundAt(draft: FoundItemDraft) = draft.foundDate?.let { date ->
        draft.foundTime?.let { time -> LocalDateTime(date, time).toInstant(timeZone) }
    }

    companion object {
        const val TITLE_MIN = 3
        const val TITLE_MAX = 60
        const val BRAND_MAX = 40
        const val FEATURES_MAX = 500
        const val MAX_QUESTIONS = 3
        const val QUESTION_MAX = 120
    }
}
