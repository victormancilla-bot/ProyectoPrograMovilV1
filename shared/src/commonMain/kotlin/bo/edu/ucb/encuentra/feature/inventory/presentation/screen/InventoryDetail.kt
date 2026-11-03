package bo.edu.ucb.encuentra.feature.inventory.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.RetentionState
import bo.edu.ucb.encuentra.core.domain.model.code
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.swatchFor
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.inventory.domain.model.CustodyEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryItemDetail
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.CustodyDraft
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryEvent
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryState
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Detalle del objeto para el staff (H07): fotos, datos, retención, custodia (editable) e historial. */
@Composable
internal fun InventoryDetail(state: InventoryState, onEvent: (InventoryEvent) -> Unit) {
    // El atrás del teléfono vuelve a la lista (el detalle no tiene ruta propia: NavRoute.kt está congelado).
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = { onEvent(InventoryEvent.OnCloseDetail) }
    )
    val detail = state.detail
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = detail?.entry?.item?.title ?: "Detalle del objeto",
                subtitle = detail?.entry?.item?.code,
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver al inventario",
                onNavigationClick = { onEvent(InventoryEvent.OnCloseDetail) },
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (detail == null) LoadingState(message = "Cargando objeto...") else DetailBody(detail, state, onEvent)
            }
        }
    }
    state.custodyDraft?.let { CustodyDialog(it, state, onEvent) }
}

@Composable
private fun DetailBody(detail: InventoryItemDetail, state: InventoryState, onEvent: (InventoryEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        RetentionBanner(detail)
        PhotosCard(detail)
        DataCard(detail)
        CustodyCard(detail, state, onEvent)
        HistoryCard(detail.history)
        Spacer(Modifier.height(spacing.medium))
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(UcbEncuentraTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
                Icon(icon, contentDescription = null, tint = UcbEncuentraTheme.colors.primary, modifier = Modifier.size(18.dp))
                AppOverline(title, color = UcbEncuentraTheme.colors.primary)
            }
            content()
        }
    }
}

/** "Resaltar por vencer": franja naranja (por vencer) o roja (vencido) arriba de todo. */
@Composable
private fun RetentionBanner(detail: InventoryItemDetail) {
    val colors = UcbEncuentraTheme.colors
    val item = detail.entry.item
    val until = item.retentionUntil.toLocalDate().formatShort()
    val days = (item.retentionUntil - Clock.System.now()).inWholeDays
    val (text, color) = when (detail.entry.retention) {
        RetentionState.VENCIDO -> "Retención vencida desde el $until: corresponde donarlo o descartarlo (D04)." to colors.error
        RetentionState.POR_VENCER -> (if (days <= 0) "La retención vence hoy ($until)." else "La retención vence en $days día(s), el $until.") to colors.warning
        RetentionState.VIGENTE -> return
    }
    Row(
        Modifier.fillMaxWidth().background(color, UcbEncuentraTheme.shapes.large).padding(UcbEncuentraTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.onPrimary)
        AppBody(text, color = colors.onPrimary)
    }
}

@Composable
private fun PhotosCard(detail: InventoryItemDetail) {
    val item = detail.entry.item
    Section("Fotos", Icons.Outlined.PhotoLibrary) {
        if (item.photoUrls.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap), verticalAlignment = Alignment.CenterVertically) {
                ItemPhoto(item, Modifier.size(72.dp))
                AppBody("Sin fotos registradas (objeto cargado antes de la captura de fotos).", modifier = Modifier.weight(1f))
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
                item.photoUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(UcbEncuentraTheme.shapes.medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun DataCard(detail: InventoryItemDetail) {
    val item = detail.entry.item
    val found = item.foundAt.toLocalDateTime(TimeZone.currentSystemDefault())
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(UcbEncuentraTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppOverline("${item.category.label} · ${item.subcategory.label}", modifier = Modifier.weight(1f))
                EntryChip(detail.entry)
            }
            AppTitle(item.title)
            Field("Código", item.code)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                swatchFor(item.color)?.let { Box(Modifier.size(12.dp).background(it.color, CircleShape)) }
                Field("Color", item.color ?: "No indicado")
            }
            Field("Marca", item.brand ?: "No indicada")
            Field("Descripción", item.description.ifBlank { "Sin descripción" })
            Field("Encontrado", "${found.date.formatShort()}, ${found.time.formatHm()} · ${item.foundZone.label}" + (item.foundDetail?.let { " · $it" } ?: ""))
            Field("Registrado por", detail.registeredByName ?: item.registeredBy)
            Field("Retención hasta", item.retentionUntil.toLocalDate().formatShort())
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppLabel(label)
        AppBody(value, color = UcbEncuentraTheme.colors.onSurface)
    }
}

@Composable
private fun CustodyCard(detail: InventoryItemDetail, state: InventoryState, onEvent: (InventoryEvent) -> Unit) {
    val item = detail.entry.item
    Section("Custodia actual", Icons.Outlined.Inventory2) {
        AppBody(
            item.custodyPlace.label + item.custodyDetail.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
            color = UcbEncuentraTheme.colors.onSurface
        )
        if (detail.canEditCustody) {
            PrimaryButton(
                text = "Editar custodia",
                onClick = { onEvent(InventoryEvent.OnEditCustody) },
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppLabel("El objeto ya salió de custodia (${item.status.label.lowercase()}).")
        }
        state.info?.let { AppLabel(it, color = UcbEncuentraTheme.colors.success) }
    }
}

@Composable
private fun HistoryCard(history: List<CustodyEntry>) {
    val zone = TimeZone.currentSystemDefault()
    Section("Historial de custodia", Icons.Outlined.History) {
        if (history.isEmpty()) AppBody("Sin movimientos.")
        history.forEachIndexed { index, entry ->
            val at = entry.event.at.toLocalDateTime(zone)
            Column(
                Modifier.fillMaxWidth().background(UcbEncuentraTheme.colors.surfaceVariant, UcbEncuentraTheme.shapes.medium)
                    .padding(UcbEncuentraTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AppLabel(if (index == 0) "Actual" else "Anterior")
                AppBody(
                    entry.event.place.label + entry.event.detail.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                    color = UcbEncuentraTheme.colors.onSurface
                )
                AppLabel("${at.date.formatShort()}, ${at.time.formatHm()} · ${entry.changedByName ?: entry.event.changedBy}")
                entry.event.note?.let { AppLabel("Nota: $it") }
            }
        }
    }
}

/** "Editar custodia": lugar (oficina, locker, bodega), detalle y nota opcional. Se audita (H05). */
@Composable
private fun CustodyDialog(draft: CustodyDraft, state: InventoryState, onEvent: (InventoryEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    AlertDialog(
        onDismissRequest = { onEvent(InventoryEvent.OnDismissCustody) },
        title = { AppTitle("Editar custodia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
                AppLabel("¿Dónde queda guardado?")
                AppChipGroup(
                    options = CustodyPlace.entries.map { it.label },
                    selected = draft.place.label,
                    onSelect = { label ->
                        CustodyPlace.entries.firstOrNull { it.label == label }?.let { onEvent(InventoryEvent.OnCustodyPlaceSelected(it)) }
                    }
                )
                AppTextField(
                    value = draft.detail,
                    onValueChange = { onEvent(InventoryEvent.OnCustodyDetailChanged(it)) },
                    label = "Detalle",
                    placeholder = "Ej. Locker 12, estante B",
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = draft.note,
                    onValueChange = { onEvent(InventoryEvent.OnCustodyNoteChanged(it)) },
                    label = "Nota (opcional)",
                    placeholder = "Ej. Se movió por limpieza",
                    modifier = Modifier.fillMaxWidth()
                )
                state.actionError?.let { AppLabel(it, color = colors.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(InventoryEvent.OnSaveCustody) }, enabled = !state.working) {
                AppLabel(if (state.working) "Guardando..." else "Guardar", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(InventoryEvent.OnDismissCustody) }) {
                AppLabel("Cancelar", color = colors.primary)
            }
        }
    )
}
