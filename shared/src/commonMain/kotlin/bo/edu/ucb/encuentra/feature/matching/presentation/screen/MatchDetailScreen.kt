package bo.edu.ucb.encuentra.feature.matching.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MatchDetailScreen(
    matchId: String,
    viewModel: MatchDetailViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Detalle de match",
        owner = "Integrante E",
        card = "M01-M05",
        detail = "matchId = $matchId",
        onNavigateBack = onNavigateBack
    )
}
