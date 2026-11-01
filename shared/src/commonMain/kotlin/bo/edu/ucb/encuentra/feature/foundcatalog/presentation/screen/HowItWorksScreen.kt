package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HowItWorksScreen(
    viewModel: HowItWorksViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Cómo funciona",
        owner = "Integrante C",
        card = "L07",
        onNavigateBack = onNavigateBack
    )
}
