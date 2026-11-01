package bo.edu.ucb.encuentra.feature.verify.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.verify.presentation.viewmodel.VerifyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VerifyScreen(
    matchId: String,
    viewModel: VerifyViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Verificación de propiedad",
        owner = "Integrante B",
        card = "V01/V02",
        detail = "matchId = $matchId",
        onNavigateBack = onNavigateBack
    )
}
