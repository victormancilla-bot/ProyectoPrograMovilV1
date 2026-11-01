package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * El ViewModel queda ligado a la entrada del back stack del wizard:
 * [onCancel] la saca del back stack y con ella se descarta el borrador.
 */
@Composable
fun ReportWizardScreen(
    viewModel: ReportWizardViewModel = koinViewModel(),
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    FeatureStub(
        title = "Reportar objeto perdido",
        owner = "Integrante C",
        card = "L02-L04",
        detail = "Paso ${state.step} de ${state.totalSteps}",
        actions = {
            Button(onClick = { viewModel.handleEvent(ReportWizardEvent.OnNext) }) { Text("Siguiente") }
            TextButton(onClick = { viewModel.handleEvent(ReportWizardEvent.OnPrevious) }) { Text("Anterior") }
        },
        backLabel = "Cancelar",
        onNavigateBack = onCancel
    )
}
