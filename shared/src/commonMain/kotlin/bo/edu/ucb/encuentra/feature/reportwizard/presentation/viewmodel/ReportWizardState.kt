package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

/** El borrador del reporte vive aquí: se pierde cuando el wizard sale del back stack (cancelar o atrás). */
data class ReportWizardState(
    val step: Int = 1,
    val totalSteps: Int = 4,
    val loading: Boolean = false,
    val error: String? = null
)
