package bo.edu.ucb.encuentra.feature.inventory.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Inventario",
        owner = "Integrante D",
        card = "H07",
        onNavigateBack = onNavigateBack
    )
}
