package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

sealed class ReportWizardEffect {
    /** Sale del wizard: la navegación lo saca del back stack y el borrador se descarta. */
    data object Cancel : ReportWizardEffect()
}
