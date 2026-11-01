package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportWizardScreen(
    viewModel: ReportWizardViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Reportar objeto perdido",
        owner = "Integrante C",
        card = "L02-L04",
        onNavigateBack = onNavigateBack
    )
}
