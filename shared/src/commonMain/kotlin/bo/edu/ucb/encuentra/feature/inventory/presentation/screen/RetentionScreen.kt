package bo.edu.ucb.encuentra.feature.inventory.presentation.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.code
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.inventory.domain.model.RetentionAction
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionEffect
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionEvent
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionState
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.RetentionViewModel
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
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
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/**
 * D04 · Retención (staff). No hay mockup: se arma con el DS como el Inventario. Vencidos con Donar / Descartar
 * (con confirmación y nota opcional) y por vencer como aviso. Se abre desde "Por vencer" del Panel y desde la
 * notificación diaria del canal Retención.
 */
@Composable
fun RetentionScreen(
    viewModel: RetentionViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RetentionEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    RetentionContent(state, viewModel::handleEvent)
}

@Composable
private fun RetentionContent(state: RetentionState, onEvent: (RetentionEvent) -> Unit) {
    val overview = state.overview
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Retención",
                subtitle = (if (overview.expired.size == 1) "1 vencido" else "${overview.expired.size} vencidos") +
                    " · ${overview.expiring.size} por vencer",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { onEvent(RetentionEvent.OnBack) },
                actions = {
                    HeaderAction(Icons.Outlined.NotificationsActive, "Revisar ahora y avisar al staff") {
                        onEvent(RetentionEvent.OnCheckNow)
                    }
                }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Revisando retención...")
                    state.error != null -> ErrorState(message = state.error)
                    else -> RetentionList(state, onEvent)
                }
            }
        }
    }
    state.pending?.let { pending -> ConfirmDialog(state, pending.item, pending.action, onEvent) }
}

@Composable
private fun RetentionList(state: RetentionState, onEvent: (RetentionEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val overview = state.overview
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        if (overview.isEmpty) {
            item {
                EmptyState(
                    title = "Nada por vencer",
                    message = "Todos los objetos en custodia están dentro de su plazo de retención.",
                    icon = Icons.Outlined.EventAvailable
                )
            }
        }
        if (overview.expired.isNotEmpty()) {
            item { AppOverline("Vencidos · decidir destino", modifier = Modifier.padding(top = spacing.medium)) }
            items(overview.expired, key = { it.id.value }) { ExpiredCard(it, state, onEvent) }
        }
        if (overview.expiring.isNotEmpty()) {
            item { AppOverline("Por vencer · 7 días", modifier = Modifier.padding(top = spacing.medium)) }
            items(overview.expiring, key = { it.id.value }) { ExpiringCard(it, state.now) }
        }
    }
}

@Composable
private fun ItemSummary(item: FoundItem, chip: String, tone: StatusTone, line: String) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap), verticalAlignment = Alignment.CenterVertically) {
        ItemPhoto(item, Modifier.size(56.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            AppTitle(item.title)
            // Dónde está guardado: el detalle de custodia ya nombra el lugar ("Oficina de Objetos Perdidos · Bloque A").
            AppLabel("${item.code} · ${item.custodyDetail.ifBlank { item.custodyPlace.label }}", color = colors.onSurfaceVariant)
            AppLabel(line, color = colors.onSurfaceVariant)
        }
        StatusChip(text = chip, tone = tone)
    }
}

@Composable
private fun ExpiredCard(item: FoundItem, state: RetentionState, onEvent: (RetentionEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            ItemSummary(item, "Vencido", StatusTone.Error, "Venció ${daysText(item.retentionUntil, state.now, past = true)}")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                SecondaryButton(
                    text = RetentionAction.DONATE.label,
                    onClick = { onEvent(RetentionEvent.OnRequest(item, RetentionAction.DONATE)) },
                    enabled = !state.working,
                    leadingIcon = Icons.Outlined.VolunteerActivism,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = RetentionAction.DISCARD.label,
                    onClick = { onEvent(RetentionEvent.OnRequest(item, RetentionAction.DISCARD)) },
                    enabled = !state.working,
                    color = colors.error,
                    leadingIcon = Icons.Outlined.DeleteOutline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExpiringCard(item: FoundItem, now: Instant) {
    AppCard(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(UcbEncuentraTheme.spacing.large)) {
            ItemSummary(item, "Por vencer", StatusTone.Warning, "Vence ${daysText(item.retentionUntil, now, past = false)}")
        }
    }
}

@Composable
private fun ConfirmDialog(state: RetentionState, item: FoundItem, action: RetentionAction, onEvent: (RetentionEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    AlertDialog(
        onDismissRequest = { onEvent(RetentionEvent.OnDismiss) },
        icon = {
            Icon(
                if (action == RetentionAction.DONATE) Icons.Outlined.VolunteerActivism else Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = if (action == RetentionAction.DONATE) colors.primary else colors.error
            )
        },
        title = { AppTitle("¿${action.label} \"${item.title}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
                AppBody("Sale de custodia y ya no se puede reclamar. Queda registrado en la auditoría.")
                AppTextField(
                    value = state.note,
                    onValueChange = { onEvent(RetentionEvent.OnNoteChanged(it)) },
                    placeholder = if (action == RetentionAction.DONATE) "Nota: a quién se donó (opcional)" else "Nota (opcional)",
                    enabled = !state.working
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(RetentionEvent.OnConfirm) }, enabled = !state.working) {
                AppLabel(action.label, color = if (action == RetentionAction.DONATE) colors.primary else colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(RetentionEvent.OnDismiss) }) { AppLabel("Cancelar", color = colors.primary) }
        }
    )
}

/** "hace 3 días (12 oct 2026)" / "en 2 días (5 nov 2026)" / "hoy". */
private fun daysText(until: Instant, now: Instant, past: Boolean): String {
    val days = (if (past) now - until else until - now).inWholeDays
    val date = until.toLocalDate().formatShort()
    val relative = when {
        days == 0L -> "hoy"
        past -> if (days == 1L) "hace 1 día" else "hace $days días"
        else -> if (days == 1L) "en 1 día" else "en $days días"
    }
    return "$relative ($date)"
}
