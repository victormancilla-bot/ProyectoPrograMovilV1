package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

sealed class ReportWizardEvent {
    data object OnNext : ReportWizardEvent()
    data object OnPrevious : ReportWizardEvent()
}
