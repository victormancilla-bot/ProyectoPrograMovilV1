package bo.edu.ucb.encuentra.feature.home.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Inicio",
        owner = "Integrante C",
        card = "L01",
        onNavigateBack = onNavigateBack
    )
}
