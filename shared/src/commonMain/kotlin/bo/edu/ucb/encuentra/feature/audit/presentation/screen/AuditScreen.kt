package bo.edu.ucb.encuentra.feature.audit.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditPeriod
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditEffect
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditEvent
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditState
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditViewModel
import edu.ucb.ds.components.cards.AppCard
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
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

private const val ALL = "Todas"

/**
 * S05 · Visor de auditoría (staff). Sin mockup en el PDF: lista agrupada por día con el DS. Solo lectura: quién
 * hizo qué, sobre qué objeto o reclamo y cuándo. Filtros por objeto (texto), persona y fecha.
 */
@Composable
fun AuditScreen(
    viewModel: AuditViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuditEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Auditoría",
                subtitle = "Registro inalterable · ${state.entries.size} eventos",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { viewModel.handleEvent(AuditEvent.OnBack) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando auditoría...")
                    state.error != null -> ErrorState(message = state.error!!)
                    else -> AuditList(state, viewModel::handleEvent)
                }
            }
        }
    }
}

@Composable
private fun AuditList(state: AuditState, onEvent: (AuditEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val visible = state.visible
    // Agrupado por día ("4 nov 2026"), del más reciente al más antiguo.
    val byDay = visible.groupBy { it.at.toLocalDateTime(state.timeZone).date }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        item { Filters(state, onEvent) }
        if (visible.isEmpty()) {
            item {
                EmptyState(
                    title = "Sin eventos",
                    message = if (state.hasFilters) "No hay eventos con esos filtros." else "Todavía no hay acciones registradas.",
                    icon = Icons.Outlined.History,
                    actionText = if (state.hasFilters) "Quitar filtros" else null,
                    onAction = { onEvent(AuditEvent.OnClearFilters) }
                )
            }
        }
        byDay.forEach { (day, entries) ->
            item(key = "day-$day") { AppOverline(day.formatShort(), modifier = Modifier.padding(top = spacing.medium)) }
            items(entries, key = { it.id }) { AuditRow(it, state) }
        }
    }
}

@Composable
private fun Filters(state: AuditState, onEvent: (AuditEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        AppTextField(
            value = state.filter.query,
            onValueChange = { onEvent(AuditEvent.OnQueryChanged(it)) },
            placeholder = "Objeto, código o n.º de reclamo",
            leadingIcon = Icons.Outlined.Search
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppOverline("Persona", modifier = Modifier.weight(1f))
            if (state.hasFilters) {
                TextButton(onClick = { onEvent(AuditEvent.OnClearFilters) }) {
                    AppLabel("Quitar filtros", color = UcbEncuentraTheme.colors.primary)
                }
            }
        }
        AppChipGroup(
            options = listOf(ALL) + state.people.map { it.name },
            selected = state.people.firstOrNull { it.id == state.filter.actorId }?.name ?: ALL,
            onSelect = { name -> onEvent(AuditEvent.OnPersonSelected(state.people.firstOrNull { it.name == name }?.id)) }
        )
        AppOverline("Fecha")
        AppChipGroup(
            options = AuditPeriod.entries.map { it.label },
            selected = state.filter.period.label,
            onSelect = { label -> AuditPeriod.entries.firstOrNull { it.label == label }?.let { onEvent(AuditEvent.OnPeriodSelected(it)) } }
        )
        AppLabel(
            if (state.visible.size == 1) "1 evento" else "${state.visible.size} eventos",
            color = UcbEncuentraTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AuditRow(entry: AuditEntry, state: AuditState) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    val time = entry.at.toLocalDateTime(state.timeZone).time.formatHm()
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.size(40.dp).background(colors.secondary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(entry.action.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTitle(entry.action.label, modifier = Modifier.weight(1f))
                    AppLabel(time, color = colors.onSurfaceVariant)
                }
                AppLabel("${entry.targetKind.label}: ${entry.targetLabel}", color = colors.tertiary)
                AppBody("Por ${entry.actorName}")
                if (entry.detail.isNotBlank()) AppLabel(entry.detail, color = colors.onSurfaceVariant)
            }
        }
    }
}

private fun AuditAction.icon(): ImageVector = when (this) {
    AuditAction.ITEM_REGISTERED -> Icons.Outlined.AddBox
    AuditAction.CUSTODY_CHANGED -> Icons.Outlined.Inventory2
    AuditAction.CLAIM_CREATED -> Icons.Outlined.Gavel
    AuditAction.CLAIM_ANSWERED -> Icons.Outlined.QuestionAnswer
    AuditAction.CLAIM_APPROVED -> Icons.Outlined.CheckCircle
    AuditAction.CLAIM_REJECTED -> Icons.Outlined.Cancel
    AuditAction.ITEM_DELIVERED -> Icons.Outlined.TaskAlt
    AuditAction.ITEM_DISCARDED -> Icons.Outlined.DeleteOutline
    AuditAction.ITEM_DONATED -> Icons.Outlined.VolunteerActivism
    AuditAction.ROLE_CHANGED -> Icons.Outlined.AdminPanelSettings
    AuditAction.REPORT_CLOSED -> Icons.Outlined.History
    AuditAction.DELIVERY_SCHEDULED -> Icons.Outlined.Event
}
