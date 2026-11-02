package bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase

import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Qué le falta a un paso del wizard para poder avanzar (vacío = el paso es válido). */
enum class ReportFieldError(val message: String) {
    CATEGORY_REQUIRED("Elige una categoría"),
    SUBCATEGORY_REQUIRED("Elige una subcategoría"),
    TITLE_TOO_SHORT("Escribe qué es (mínimo ${ValidateReportStepUseCase.TITLE_MIN} letras)"),
    TITLE_TOO_LONG("Máximo ${ValidateReportStepUseCase.TITLE_MAX} caracteres"),
    COLOR_REQUIRED("Elige el color principal"),
    BRAND_TOO_LONG("Máximo ${ValidateReportStepUseCase.BRAND_MAX} caracteres"),
    FEATURES_TOO_LONG("Máximo ${ValidateReportStepUseCase.FEATURES_MAX} caracteres"),
    ZONE_REQUIRED("Elige dónde lo perdiste"),
    DETAIL_TOO_LONG("Máximo ${ValidateReportStepUseCase.DETAIL_MAX} caracteres"),
    DATE_REQUIRED("Indica desde y hasta cuándo pudo perderse"),
    DATE_RANGE_INVERTED("\"Hasta\" no puede ser antes de \"Desde\""),
    DATE_IN_FUTURE("Las fechas no pueden ser futuras"),
    DATE_RANGE_TOO_WIDE("El rango puede ser de ${ValidateReportStepUseCase.MAX_RANGE_DAYS} días como máximo"),
    TIME_SLOT_REQUIRED("Elige la franja horaria"),
    PHONE_INVALID("Número no válido (ej. 71234567 o +591 71234567)")
}

/**
 * Validación por paso del wizard (L02-L03). El botón Siguiente se habilita solo si el paso no tiene errores.
 * El paso 4 es la confirmación: no agrega campos.
 */
class ValidateReportStepUseCase(
    private val clock: Clock,
    private val timeZone: TimeZone
) {

    operator fun invoke(step: Int, draft: ReportDraft): Set<ReportFieldError> = when (step) {
        STEP_CATEGORY -> categoryErrors(draft)
        STEP_ATTRIBUTES -> attributeErrors(draft)
        STEP_WHERE_WHEN -> whereWhenErrors(draft)
        else -> emptySet()
    }

    /** Errores de los pasos 1 a [lastStep]: el envío (L04) revisa todo el borrador. */
    fun upTo(lastStep: Int, draft: ReportDraft): Set<ReportFieldError> =
        (STEP_CATEGORY..lastStep).flatMap { invoke(it, draft) }.toSet()

    private fun categoryErrors(draft: ReportDraft): Set<ReportFieldError> = buildSet {
        if (draft.category == null) add(ReportFieldError.CATEGORY_REQUIRED)
        val sub = draft.subcategory
        if (sub == null || sub.category != draft.category) add(ReportFieldError.SUBCATEGORY_REQUIRED)
    }

    private fun attributeErrors(draft: ReportDraft): Set<ReportFieldError> = buildSet {
        val title = draft.title.trim()
        if (title.length < TITLE_MIN) add(ReportFieldError.TITLE_TOO_SHORT)
        if (title.length > TITLE_MAX) add(ReportFieldError.TITLE_TOO_LONG)
        if (draft.color.isNullOrBlank()) add(ReportFieldError.COLOR_REQUIRED)
        if (draft.brand.trim().length > BRAND_MAX) add(ReportFieldError.BRAND_TOO_LONG)
        if (draft.features.trim().length > FEATURES_MAX) add(ReportFieldError.FEATURES_TOO_LONG)
    }

    private fun whereWhenErrors(draft: ReportDraft): Set<ReportFieldError> = buildSet {
        if (draft.zone == null) add(ReportFieldError.ZONE_REQUIRED)
        if (draft.locationDetail.trim().length > DETAIL_MAX) add(ReportFieldError.DETAIL_TOO_LONG)

        val from = draft.lostFrom
        val to = draft.lostTo
        val today = clock.now().toLocalDateTime(timeZone).date
        when {
            from == null || to == null -> add(ReportFieldError.DATE_REQUIRED)
            to < from -> add(ReportFieldError.DATE_RANGE_INVERTED)
            to > today -> add(ReportFieldError.DATE_IN_FUTURE)
            from.daysUntil(to) >= MAX_RANGE_DAYS -> add(ReportFieldError.DATE_RANGE_TOO_WIDE)
        }
        if (draft.timeSlot == null) add(ReportFieldError.TIME_SLOT_REQUIRED)

        val phone = draft.contactPhone.filterNot { it == ' ' || it == '-' }
        if (phone.isNotEmpty() && !PHONE_REGEX.matches(phone)) add(ReportFieldError.PHONE_INVALID)
    }

    /**
     * Rango desde/hasta como instantes: inicio de la franja el primer día y fin de la franja el último
     * (lo que guarda L04 en `lostFrom`/`lostTo`). Null si falta algún dato.
     */
    fun lostRange(draft: ReportDraft): Pair<Instant, Instant>? {
        val from = draft.lostFrom ?: return null
        val to = draft.lostTo ?: return null
        val slot = draft.timeSlot ?: return null
        return LocalDateTime(from, slot.start).toInstant(timeZone) to LocalDateTime(to, slot.end).toInstant(timeZone)
    }

    /** Hoy en la zona del dispositivo: valor inicial de las fechas del paso 3. */
    fun today() = clock.now().toLocalDateTime(timeZone).date

    companion object {
        const val STEP_CATEGORY = 1
        const val STEP_ATTRIBUTES = 2
        const val STEP_WHERE_WHEN = 3
        const val TITLE_MIN = 3
        const val TITLE_MAX = 60
        const val BRAND_MAX = 40
        const val FEATURES_MAX = 500
        const val DETAIL_MAX = 120
        const val MAX_RANGE_DAYS = 30

        /** Celular o fijo de Bolivia (8 dígitos), con +591 opcional. */
        private val PHONE_REGEX = Regex("""^(\+?591)?[2-7]\d{7}$""")
    }
}
