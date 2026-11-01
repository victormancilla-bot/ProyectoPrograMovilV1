package bo.edu.ucb.encuentra.feature.audit.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuditScreen(
    viewModel: AuditViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Auditoría",
        owner = "Integrante B",
        card = "S05",
        onNavigateBack = onNavigateBack
    )
}
