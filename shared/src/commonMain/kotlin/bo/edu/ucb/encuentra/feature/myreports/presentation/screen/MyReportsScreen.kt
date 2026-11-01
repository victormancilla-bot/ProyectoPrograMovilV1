package bo.edu.ucb.encuentra.feature.myreports.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Mis reportes",
        owner = "Integrante C",
        card = "L05",
        onNavigateBack = onNavigateBack
    )
}
