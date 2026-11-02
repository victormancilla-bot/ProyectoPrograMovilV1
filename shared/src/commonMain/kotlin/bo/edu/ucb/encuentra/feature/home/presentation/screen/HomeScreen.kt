package bo.edu.ucb.encuentra.feature.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeEffect
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeEvent
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeState
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeViewModel
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.list.AppListGroup
import edu.ucb.ds.components.list.AppListItem
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Home del estudiante (Figma "Home Estudiante", L01). */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToReportWizard: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToFoundCatalog: () -> Unit,
    onNavigateToHowItWorks: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToReportWizard -> onNavigateToReportWizard()
                HomeEffect.NavigateToMyReports -> onNavigateToMyReports()
                HomeEffect.NavigateToMatches -> onNavigateToMatches()
                HomeEffect.NavigateToFoundCatalog -> onNavigateToFoundCatalog()
                HomeEffect.NavigateToHowItWorks -> onNavigateToHowItWorks()
            }
        }
    }

    HomeContent(state = state, onEvent = viewModel::handleEvent)
}

@Composable
private fun HomeContent(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CurvedHeader(title = state.greeting, subtitle = state.subtitle)

            Column(
                modifier = Modifier.padding(horizontal = spacing.screenPadding, vertical = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                LostSomethingCard(onReport = { onEvent(HomeEvent.OnReportObject) })

                AppOverline("Actividad", modifier = Modifier.padding(start = spacing.medium, top = spacing.large))
                ActivityCard(state = state, onEvent = onEvent)

                AppOverline("Accesos rápidos", modifier = Modifier.padding(start = spacing.medium, top = spacing.large))
                AppListGroup {
                    AppListItem("Mis reportes", leadingIcon = Icons.Outlined.Assignment, onClick = {
                        onEvent(HomeEvent.OnMyReportsClick)
                    })
                    AppListItem("Objetos encontrados", leadingIcon = Icons.Outlined.Inventory2, onClick = {
                        onEvent(HomeEvent.OnFoundItemsClick)
                    })
                    AppListItem("Cómo funciona", leadingIcon = Icons.Outlined.HelpOutline, onClick = {
                        onEvent(HomeEvent.OnHowItWorksClick)
                    })
                }
                Spacer(Modifier.height(spacing.large))
            }
        }
    }
}

/** Tarjeta "¿Perdiste algo?" con el botón "Reportar objeto". */
@Composable
private fun LostSomethingCard(onReport: () -> Unit) {
    AppCard(variant = AppCardVariant.Filled) {
        Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
            AppHeading("¿Perdiste algo?")
            AppBody("La comunidad UCB está aquí para ayudarte a recuperarlo.")
        }
        PrimaryButton(
            text = "Reportar objeto",
            onClick = onReport,
            leadingIcon = Icons.Filled.AddCircle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Sección "Actividad": reportes activos y coincidencias nuevas (con punto rojo si hay). */
@Composable
private fun ActivityCard(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    val c = UcbEncuentraTheme.colors
    AppCard(variant = AppCardVariant.Filled) {
        ActivityRow(
            text = if (state.loading) "Cargando…" else state.activeReportsText,
            icon = Icons.Filled.Description,
            iconBackground = c.tertiary,
            iconTint = c.onTertiary,
            showDot = false,
            onClick = { onEvent(HomeEvent.OnActiveReportsClick) }
        )
        HorizontalDivider(color = c.outlineVariant.copy(alpha = DIVIDER_ALPHA))
        ActivityRow(
            text = if (state.loading) "Cargando…" else state.newMatchesText,
            icon = Icons.Filled.TravelExplore,
            iconBackground = c.surfaceVariant,
            iconTint = c.onSurfaceVariant,
            showDot = state.hasNewMatches,
            onClick = { onEvent(HomeEvent.OnNewMatchesClick) }
        )
    }
}

@Composable
private fun ActivityRow(
    text: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    showDot: Boolean,
    onClick: () -> Unit
) {
    val c = UcbEncuentraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
    ) {
        Box {
            Box(
                modifier = Modifier.size(40.dp).background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            if (showDot) {
                // Punto rojo del Figma: 16, error, con borde del color de la tarjeta.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(16.dp)
                        .background(c.error, CircleShape)
                        .border(2.dp, c.surfaceContainer, CircleShape)
                )
            }
        }
        Text(
            text = text,
            style = UcbEncuentraTheme.typography.title.copy(fontWeight = UcbEncuentraTheme.typography.label.fontWeight),
            color = c.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(DsIcons.ChevronRight, contentDescription = null, tint = c.onSurfaceVariant)
    }
}

private const val DIVIDER_ALPHA = 0.3f
