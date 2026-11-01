package bo.edu.ucb.encuentra.feature.matching.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Posibles coincidencias",
        owner = "Integrante E",
        card = "M01-M05",
        onNavigateBack = onNavigateBack
    )
}
