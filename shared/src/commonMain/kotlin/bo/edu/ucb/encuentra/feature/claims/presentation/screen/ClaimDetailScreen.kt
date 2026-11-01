package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClaimDetailScreen(
    claimId: String,
    viewModel: ClaimDetailViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Detalle de reclamo",
        owner = "Integrante A",
        card = "V03",
        detail = "claimId = $claimId",
        onNavigateBack = onNavigateBack
    )
}
