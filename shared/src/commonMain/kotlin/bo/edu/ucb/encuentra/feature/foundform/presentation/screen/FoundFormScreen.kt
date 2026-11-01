package bo.edu.ucb.encuentra.feature.foundform.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FoundFormScreen(
    viewModel: FoundFormViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Registrar objeto",
        owner = "Integrante D",
        card = "H03/H05",
        onNavigateBack = onNavigateBack
    )
}
