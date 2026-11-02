package bo.edu.ucb.encuentra.feature.myreports.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportFilter
import bo.edu.ucb.encuentra.feature.myreports.domain.model.isActive
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsEffect
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsEvent
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsState
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsViewModel
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Mis reportes (L05, PDF "Mis Reportes"): lista con filtros y, al tocar uno, su detalle en la misma pantalla. */
@Composable
fun MyReportsScreen(
    viewModel: MyReportsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    onSeeAllMatches: () -> Unit,
    onReportLost: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyReportsEffect.NavigateToMatchDetail -> onOpenMatch(effect.matchId)
                MyReportsEffect.NavigateToMatchList -> onSeeAllMatches()
                MyReportsEffect.NavigateToReportWizard -> onReportLost()
            }
        }
    }

    // El botón atrás del teléfono cierra el detalle antes de salir de la pantalla.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isDetailOpen,
        onBackCompleted = { viewModel.handleEvent(MyReportsEvent.OnBackFromDetail) }
    )

    if (state.isDetailOpen) {
        ReportDetailContent(state = state, onEvent = viewModel::handleEvent)
    } else {
        ReportListContent(state = state, onEvent = viewModel::handleEvent, onNavigateBack = onNavigateBack)
    }
}

@Composable
private fun ReportListContent(state: MyReportsState, onEvent: (MyReportsEvent) -> Unit, onNavigateBack: () -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Mis reportes",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = onNavigateBack,
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando tus reportes...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(MyReportsEvent.OnRetry) })
                    state.isEmpty -> EmptyState(
                        title = "Aún no reportaste nada",
                        message = "Si perdiste algo en el campus, repórtalo y te avisaremos cuando aparezca algo parecido.",
                        actionText = "Reportar objeto perdido",
                        onAction = { onEvent(MyReportsEvent.OnReportLost) }
                    )
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.screenPadding, vertical = spacing.large),
                        verticalArrangement = Arrangement.spacedBy(spacing.large)
                    ) {
                        AppChipGroup(
                            options = ReportFilter.entries.map { it.label },
                            selected = state.filter.label,
                            onSelect = { label ->
                                ReportFilter.entries.firstOrNull { it.label == label }
                                    ?.let { onEvent(MyReportsEvent.OnFilterSelected(it)) }
                            }
                        )
                        if (state.visibleReports.isEmpty()) {
                            EmptyState(
                                title = "Nada en \"${state.filter.label}\"",
                                message = "No tienes reportes con este estado. Prueba con \"Todos\"."
                            )
                        }
                        state.visibleReports.forEach { report ->
                            ItemCard(
                                category = report.category.label,
                                title = report.title,
                                date = "Reportado: ${report.createdAt.toLocalDate().formatShort()}",
                                location = report.locationText,
                                status = report.status.statusChip(),
                                actionText = "Ver detalles",
                                onAction = { onEvent(MyReportsEvent.OnOpenReport(report.id)) },
                                // Como "Botella de Agua Azul" del mockup: los que ya no están en curso, atenuados.
                                dimmed = !report.status.isActive,
                                onClick = { onEvent(MyReportsEvent.OnOpenReport(report.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
