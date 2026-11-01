package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClaimsScreen(
    viewModel: ClaimsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Reclamos",
        owner = "Integrante A",
        card = "V03",
        onNavigateBack = onNavigateBack
    )
}
