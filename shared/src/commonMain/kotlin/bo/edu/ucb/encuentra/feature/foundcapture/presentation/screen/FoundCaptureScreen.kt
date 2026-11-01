package bo.edu.ucb.encuentra.feature.foundcapture.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FoundCaptureScreen(
    viewModel: FoundCaptureViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Captura de foto",
        owner = "Integrante D",
        card = "H01",
        onNavigateBack = onNavigateBack
    )
}
