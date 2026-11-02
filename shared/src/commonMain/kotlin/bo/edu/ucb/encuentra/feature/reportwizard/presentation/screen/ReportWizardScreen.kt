package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportDraftState
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEffect
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.progress.StepProgressBar
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wizard "Reportar objeto" (L02-L04). El ViewModel queda ligado a la entrada del back stack del wizard:
 * [onCancel] la saca del back stack y con ella se descarta el borrador.
 */
@Composable
fun ReportWizardScreen(
    viewModel: ReportWizardViewModel = koinViewModel(),
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReportWizardEffect.Cancel -> onCancel()
            }
        }
    }

    ReportWizardContent(state = state, onEvent = viewModel::handleEvent)
}

@Composable
private fun ReportWizardContent(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            CurvedHeader(
                title = "Reportar objeto",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = if (state.step == 1) "Cancelar" else "Paso anterior",
                onNavigationClick = { onEvent(ReportWizardEvent.OnBack) },
                centerTitle = true,
                bottomContent = {
                    StepProgressBar(
                        currentStep = state.step,
                        totalSteps = state.totalSteps,
                        stepName = state.stepName,
                        onDark = true
                    )
                }
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenPadding, vertical = spacing.large)
            ) {
                when (state.step) {
                    1 -> CategoryStep(state = state, onEvent = onEvent)
                    2 -> AttributesStep(state = state, onEvent = onEvent)
                    3 -> WhereWhenStep(state = state, onEvent = onEvent)
                    else -> AppBody(
                        "Paso ${state.step} · ${state.stepName}: llega con L03/L04.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Barra inferior fija del Figma: Siguiente deshabilitado (al 50 %) hasta que el paso sea válido.
            Box(
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .background(UcbEncuentraTheme.colors.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(spacing.screenPadding)
            ) {
                PrimaryButton(
                    text = "Siguiente",
                    onClick = { onEvent(ReportWizardEvent.OnNext) },
                    enabled = state.canContinue,
                    size = AppButtonSize.Large,
                    trailingIcon = DsIcons.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
