package bo.edu.ucb.encuentra.feature.matching.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchCandidate
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListEffect
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListEvent
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListState
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Figma "Resultados de Matching" (M04): candidatos con su % de parecido, nunca un "es / no es". */
@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    onModifyCriteria: () -> Unit,
    onReportLost: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MatchListEffect.NavigateToMatchDetail -> onOpenMatch(effect.matchId)
                MatchListEffect.NavigateToMyReports -> onModifyCriteria()
                MatchListEffect.NavigateToReportWizard -> onReportLost()
            }
        }
    }

    MatchListContent(state = state, onEvent = viewModel::handleEvent, onNavigateBack = onNavigateBack)
}

@Composable
private fun MatchListContent(state: MatchListState, onEvent: (MatchListEvent) -> Unit, onNavigateBack: () -> Unit) {
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Posibles coincidencias",
                subtitle = "Revisa estos objetos encontrados",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = onNavigateBack,
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val selected = state.selected
                when {
                    state.loading -> LoadingState(message = "Buscando coincidencias...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(MatchListEvent.OnRetry) })
                    selected == null -> EmptyState(
                        title = "Aún no hay coincidencias",
                        message = "Cuando el staff registre un objeto parecido a lo que reportaste, aparecerá aquí con su porcentaje de parecido.",
                        actionText = "Reportar objeto perdido",
                        onAction = { onEvent(MatchListEvent.OnReportLost) }
                    )
                    else -> MatchResults(state.groups, selected, state.rejectError, onEvent)
                }
            }
        }
    }

    if (state.pendingReject != null) {
        AlertDialog(
            onDismissRequest = { onEvent(MatchListEvent.OnDismissReject) },
            title = { AppTitle("¿No es tuyo?") },
            text = { AppBody("Este objeto no volverá a aparecer como coincidencia de tu reporte.") },
            confirmButton = {
                TextButton(onClick = { onEvent(MatchListEvent.OnConfirmReject) }) {
                    AppLabel("No es mío", color = UcbEncuentraTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MatchListEvent.OnDismissReject) }) {
                    AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun MatchResults(
    groups: List<ReportMatches>,
    selected: ReportMatches,
    rejectError: String?,
    onEvent: (MatchListEvent) -> Unit
) {
    val spacing = UcbEncuentraTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding, vertical = spacing.large),
        // Espacio extra entre tarjetas: el badge de score sobresale de la esquina.
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        if (groups.size > 1) {
            AppOverline("Tus reportes")
            AppChipGroup(
                options = groups.map { it.report.title },
                selected = selected.report.title,
                onSelect = { title ->
                    groups.firstOrNull { it.report.title == title }?.let { onEvent(MatchListEvent.OnReportSelected(it.report.id)) }
                }
            )
        }
        val count = selected.candidates.size
        AppBody(
            if (count == 1) {
                "Para \"${selected.report.title}\": 1 candidato."
            } else {
                "Para \"${selected.report.title}\": $count candidatos, del más parecido al menos parecido."
            }
        )

        selected.candidates.forEachIndexed { index, candidate ->
            CandidateCard(
                rank = index + 1,
                candidate = candidate,
                onClick = { onEvent(MatchListEvent.OnMatchClicked(candidate.match.id)) },
                onReject = { onEvent(MatchListEvent.OnRejectMatch(candidate.match.id)) }
            )
        }
        rejectError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }

        AppLabel("El porcentaje indica qué tanto se parece cada objeto a tu reporte. El staff verifica antes de entregar.")

        PrimaryButton(
            text = "Modificar criterios",
            onClick = { onEvent(MatchListEvent.OnModifyCriteria) },
            size = AppButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CandidateCard(rank: Int, candidate: MatchCandidate, onClick: () -> Unit, onReject: () -> Unit) {
    // "No es mío" solo en las propuestas: una coincidencia confirmada ya la revisó el staff.
    val proposed = candidate.match.status == MatchStatus.PROPUESTO
    val item = candidate.item
    ItemCard(
        category = "#$rank · ${item.category.label}",
        title = item.title,
        // El estado va en la línea de fecha: la esquina superior derecha es del badge de score.
        date = "Encontrado: ${item.foundAt.toLocalDate().formatShort()}" +
            if (candidate.match.status == MatchStatus.CONFIRMADO) " · Confirmado" else "",
        image = { ItemPhoto(item.photoUrls.firstOrNull()) },
        score = candidate.match.score,
        actionText = if (proposed) "No es mío" else null,
        onAction = onReject,
        variant = AppCardVariant.Filled,
        onClick = onClick
    )
}

/** Foto del objeto, o el ícono "sin imagen" del mockup (el seed 2026 no trae fotos). */
@Composable
private fun ItemPhoto(url: String?) {
    val colors = UcbEncuentraTheme.colors
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(UcbEncuentraTheme.shapes.small)
        )
    } else {
        Box(
            Modifier.fillMaxSize().background(colors.surfaceVariant, UcbEncuentraTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.HideImage, contentDescription = "Sin foto", tint = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}
