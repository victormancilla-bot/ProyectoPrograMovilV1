package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FoundCatalogScreen(
    viewModel: FoundCatalogViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    FeatureStub(
        title = "Objetos encontrados",
        owner = "Integrante C",
        card = "L07",
        onNavigateBack = onNavigateBack
    )
}
