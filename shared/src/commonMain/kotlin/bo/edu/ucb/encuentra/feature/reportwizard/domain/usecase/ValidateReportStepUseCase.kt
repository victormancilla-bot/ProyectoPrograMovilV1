package bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase

import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft

/** Qué le falta a un paso del wizard para poder avanzar (vacío = el paso es válido). */
enum class ReportFieldError(val message: String) {
    CATEGORY_REQUIRED("Elige una categoría"),
    SUBCATEGORY_REQUIRED("Elige una subcategoría"),
    TITLE_TOO_SHORT("Escribe qué es (mínimo ${ValidateReportStepUseCase.TITLE_MIN} letras)"),
    TITLE_TOO_LONG("Máximo ${ValidateReportStepUseCase.TITLE_MAX} caracteres"),
    COLOR_REQUIRED("Elige el color principal"),
    BRAND_TOO_LONG("Máximo ${ValidateReportStepUseCase.BRAND_MAX} caracteres"),
    FEATURES_TOO_LONG("Máximo ${ValidateReportStepUseCase.FEATURES_MAX} caracteres")
}

/**
 * Validación por paso del wizard (L02). El botón Siguiente se habilita solo si el paso no tiene errores.
 * Pasos 3 y 4 los valida L03/L04.
 */
class ValidateReportStepUseCase {

    operator fun invoke(step: Int, draft: ReportDraft): Set<ReportFieldError> = when (step) {
        STEP_CATEGORY -> categoryErrors(draft)
        STEP_ATTRIBUTES -> attributeErrors(draft)
        else -> emptySet()
    }

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

    companion object {
        const val STEP_CATEGORY = 1
        const val STEP_ATTRIBUTES = 2
        const val TITLE_MIN = 3
        const val TITLE_MAX = 60
        const val BRAND_MAX = 40
        const val FEATURES_MAX = 500
    }
}
