package bo.edu.ucb.encuentra.feature.alerts.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Alertas",
        owner = "Integrante C",
        card = "N01-N04",
        onNavigateBack = onNavigateBack
    )
}
