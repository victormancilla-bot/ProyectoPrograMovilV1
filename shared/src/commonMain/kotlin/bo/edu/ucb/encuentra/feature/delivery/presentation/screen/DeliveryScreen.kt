package bo.edu.ucb.encuentra.feature.delivery.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeliveryScreen(
    claimId: String,
    viewModel: DeliveryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Confirmación de entrega",
        owner = "Integrante B",
        card = "V04",
        detail = "claimId = $claimId",
        onNavigateBack = onNavigateBack
    )
}
