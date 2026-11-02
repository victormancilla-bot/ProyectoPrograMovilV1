package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

sealed class MatchListEffect {
    data class NavigateToMatchDetail(val matchId: String) : MatchListEffect()
    data object NavigateToMyReports : MatchListEffect()
    data object NavigateToReportWizard : MatchListEffect()
}
