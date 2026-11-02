package bo.edu.ucb.encuentra.feature.inventory.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.icons.DsIcons
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onRegisterFound: () -> Unit
) {
    FeatureStub(
        title = "Inventario",
        owner = "Integrante D",
        card = "H07",
        // Acceso temporal al registro (H03) hasta que H01 (captura de foto) abra el flujo.
        actions = {
            PrimaryButton(text = "Registrar objeto encontrado", onClick = onRegisterFound, leadingIcon = DsIcons.Add)
        },
        onNavigateBack = onNavigateBack
    )
}
