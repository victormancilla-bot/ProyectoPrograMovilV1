package bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel

sealed class MyReportsEffect {
    data class NavigateToMatchDetail(val matchId: String) : MyReportsEffect()
    data object NavigateToMatchList : MyReportsEffect()
    data object NavigateToReportWizard : MyReportsEffect()
}
