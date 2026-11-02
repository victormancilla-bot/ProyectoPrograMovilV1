package bo.edu.ucb.encuentra.feature.home.presentation.viewmodel

sealed class HomeEffect {
    data object NavigateToReportWizard : HomeEffect()
    data object NavigateToMyReports : HomeEffect()
    data object NavigateToMatches : HomeEffect()
    data object NavigateToFoundCatalog : HomeEffect()
    data object NavigateToHowItWorks : HomeEffect()
}
