package bo.edu.ucb.encuentra.feature.alerts.presentation.screen

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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.presentation.formatRelative
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsEffect
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsEvent
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsFilter
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsState
import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsViewModel
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.header.HeaderAction
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bandeja de Alertas (N04). El PDF no tiene esta pantalla: se arma con el DS como el resto de listas
 * (header curvo, chips de filtro, tarjetas). Las no leídas van primero, con fondo y un punto.
 */
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = koinViewModel(),
    onOpenLink: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AlertsEffect.OpenLink -> onOpenLink(effect.uri)
            }
        }
    }

    AlertsContent(state, viewModel::handleEvent)
}

@Composable
private fun AlertsContent(state: AlertsState, onEvent: (AlertsEvent) -> Unit) {
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Alertas",
                subtitle = when (state.unreadCount) {
                    0 -> "Estás al día"
                    1 -> "1 sin leer"
                    else -> "${state.unreadCount} sin leer"
                },
                actions = {
                    if (state.unreadCount > 0) {
                        HeaderAction(Icons.Outlined.DoneAll, "Marcar todas como leídas") { onEvent(AlertsEvent.OnMarkAllRead) }
                    }
                    if (state.readCount > 0) {
                        HeaderAction(Icons.Outlined.DeleteSweep, "Limpiar leídas") { onEvent(AlertsEvent.OnRequestClear) }
                    }
                }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando alertas...")
                    state.error != null -> ErrorState(message = state.error)
                    state.alerts.isEmpty() -> EmptyState(
                        title = "No tienes alertas",
                        message = "Aquí te avisamos de coincidencias con tus reportes y de cambios en tus reclamos.",
                        icon = Icons.Outlined.Notifications
                    )
                    else -> AlertsList(state, onEvent)
                }
            }
        }
    }
    if (state.confirmClear) {
        AlertDialog(
            onDismissRequest = { onEvent(AlertsEvent.OnDismissClear) },
            title = { AppTitle("¿Limpiar alertas leídas?") },
            text = { AppBody("Se borran las ${state.readCount} alertas que ya leíste. Las no leídas se quedan.") },
            confirmButton = {
                TextButton(onClick = { onEvent(AlertsEvent.OnConfirmClear) }) {
                    AppLabel("Limpiar", color = UcbEncuentraTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AlertsEvent.OnDismissClear) }) {
                    AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun AlertsList(state: AlertsState, onEvent: (AlertsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        item {
            AppChipGroup(
                options = AlertsFilter.entries.map { chipLabel(it, state) },
                selected = chipLabel(state.filter, state),
                onSelect = { label ->
                    AlertsFilter.entries.firstOrNull { chipLabel(it, state) == label }
                        ?.let { onEvent(AlertsEvent.OnFilterChanged(it)) }
                }
            )
        }
        if (state.unread.isEmpty() && state.filter == AlertsFilter.NO_LEIDAS) {
            item { EmptyState(title = "Estás al día", message = "No tienes alertas sin leer.", icon = Icons.Outlined.DoneAll) }
        }
        if (state.unread.isNotEmpty()) {
            item { AppOverline("Nuevas", modifier = Modifier.padding(top = spacing.medium)) }
            items(state.unread, key = { it.id.value }) { AlertCard(it, state, onEvent) }
        }
        if (state.earlier.isNotEmpty()) {
            item { AppOverline("Anteriores", modifier = Modifier.padding(top = spacing.medium)) }
            items(state.earlier, key = { it.id.value }) { AlertCard(it, state, onEvent) }
        }
    }
}

private fun chipLabel(filter: AlertsFilter, state: AlertsState) = "${filter.label} (${state.countFor(filter)})"

@Composable
private fun AlertCard(alert: AppNotification, state: AlertsState, onEvent: (AlertsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    AppCard(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = (if (alert.read) "" else "No leída. ") + "${alert.title}. ${alert.body}"
        },
        // Las no leídas resaltan (fondo de tarjeta del Figma); las leídas quedan en blanco.
        variant = if (alert.read) AppCardVariant.Elevated else AppCardVariant.Filled,
        onClick = { onEvent(AlertsEvent.OnOpen(alert)) }
    ) {
        Row(
            Modifier.padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier.size(40.dp).background(colors.secondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(alert.type.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLabel(alert.type.label, color = colors.tertiary, modifier = Modifier.weight(1f))
                    AppLabel(formatRelative(alert.createdAt, state.now), color = colors.onSurfaceVariant)
                }
                AppTitle(alert.title)
                AppBody(alert.body)
                if (!alert.read) {
                    TextButton(
                        onClick = { onEvent(AlertsEvent.OnMarkRead(alert)) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        AppLabel("Marcar como leída", color = colors.primary)
                    }
                }
            }
            if (!alert.read) {
                Box(Modifier.padding(top = spacing.small).size(10.dp).background(colors.error, CircleShape))
            }
        }
    }
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.NEW_MATCH -> Icons.Outlined.TravelExplore
    NotificationType.CLAIM_UPDATE -> Icons.Outlined.Assignment
    NotificationType.RETENTION -> Icons.Outlined.Schedule
    NotificationType.GENERAL -> Icons.Outlined.Notifications
}
