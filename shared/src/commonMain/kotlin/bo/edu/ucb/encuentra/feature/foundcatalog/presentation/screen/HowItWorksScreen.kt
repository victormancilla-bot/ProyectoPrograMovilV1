package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QuestionAnswer
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.HowItWorksStep
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksEffect
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksEvent
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksViewModel
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Un ícono por paso (mismo orden que GetHowItWorksUseCase). */
private val STEP_ICONS: List<ImageVector> = listOf(
    Icons.Outlined.EditNote,
    Icons.Outlined.NotificationsActive,
    Icons.Outlined.QuestionAnswer,
    Icons.Outlined.Badge
)

/** L07 · "Cómo funciona" en 4 pasos (sin pantalla en el PDF: tarjetas numeradas del DS). */
@Composable
fun HowItWorksScreen(
    viewModel: HowItWorksViewModel = koinViewModel(),
    onReport: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HowItWorksEffect.NavigateToReport -> onReport()
                HowItWorksEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Cómo funciona",
                subtitle = "De la pérdida a la entrega",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { viewModel.handleEvent(HowItWorksEvent.OnBack) }
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
            ) {
                state.steps.forEachIndexed { index, step -> StepCard(index + 1, step, STEP_ICONS.getOrElse(index) { STEP_ICONS.last() }) }
            }
            Box(Modifier.padding(spacing.screenPadding)) {
                PrimaryButton(
                    text = "Reportar objeto",
                    onClick = { viewModel.handleEvent(HowItWorksEvent.OnReport) },
                    size = AppButtonSize.Large,
                    leadingIcon = Icons.Outlined.AddCircleOutline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepCard(number: Int, step: HowItWorksStep, icon: ImageVector) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.large),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.size(44.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                AppLabel("PASO $number", color = colors.tertiary)
                AppTitle(step.title)
                AppBody(step.body)
            }
        }
    }
}
