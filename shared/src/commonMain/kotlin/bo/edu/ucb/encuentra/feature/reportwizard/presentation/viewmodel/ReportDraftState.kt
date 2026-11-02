package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError

/**
 * Estado compartido por los 4 pasos del wizard: el paso actual, el borrador y lo que le falta al paso.
 * El borrador vive en el ViewModel, ligado a la entrada del wizard en el back stack: cancelar lo descarta.
 */
data class ReportDraftState(
    val step: Int = 1,
    val totalSteps: Int = TOTAL_STEPS,
    val draft: ReportDraft = ReportDraft(),
    /** Errores del paso actual; Siguiente queda deshabilitado mientras haya alguno. */
    val errors: Set<ReportFieldError> = emptySet(),
    /** "+" del selector de color: muestra la paleta completa. */
    val showAllColors: Boolean = false
) {
    val canContinue: Boolean get() = errors.isEmpty()

    val stepName: String get() = STEP_NAMES.getOrElse(step - 1) { "" }

    companion object {
        const val TOTAL_STEPS = 4
        private val STEP_NAMES = listOf("Categoría", "Atributos", "Dónde y cuándo", "Confirmación")
    }
}
