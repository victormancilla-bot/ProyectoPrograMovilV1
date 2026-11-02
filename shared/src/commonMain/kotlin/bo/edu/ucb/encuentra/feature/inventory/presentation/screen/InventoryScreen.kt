package bo.edu.ucb.encuentra.feature.inventory.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.RetentionState
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryFilter
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryEffect
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryEvent
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryState
import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Figma "Inventario" (H07): buscar por texto o código, chips por estado y detalle de cada objeto. */
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = koinViewModel(),
    onRegisterFound: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InventoryEffect.NavigateToFoundForm -> onRegisterFound()
            }
        }
    }

    if (state.selectedId != null) {
        InventoryDetail(state, viewModel::handleEvent)
    } else {
        InventoryList(state, viewModel::handleEvent)
    }
}

@Composable
private fun InventoryList(state: InventoryState, onEvent: (InventoryEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Inventario de objetos",
                subtitle = state.campus.takeIf { it.isNotEmpty() }?.let { "UCB $it" },
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando inventario...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(InventoryEvent.OnRetry) })
                    else -> LazyColumn(
                        contentPadding = PaddingValues(spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.large)
                    ) {
                        item {
                            AppTextField(
                                value = state.query,
                                onValueChange = { onEvent(InventoryEvent.OnQueryChanged(it)) },
                                placeholder = "Buscar objetos, códigos...",
                                leadingIcon = Icons.Outlined.Search,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            AppChipGroup(
                                options = InventoryFilter.entries.map { it.label },
                                selected = state.filter.label,
                                onSelect = { label ->
                                    InventoryFilter.entries.firstOrNull { it.label == label }?.let { onEvent(InventoryEvent.OnFilterSelected(it)) }
                                }
                            )
                        }
                        item {
                            SecondaryButton(
                                text = "Registrar objeto encontrado",
                                onClick = { onEvent(InventoryEvent.OnRegisterFound) },
                                leadingIcon = DsIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        val visible = state.visible
                        item { AppOverline("Inventario reciente · ${visible.size}") }
                        if (visible.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Sin resultados",
                                    message = if (state.query.isBlank()) "No hay objetos en \"${state.filter.label}\"." else "Ningún objeto coincide con \"${state.query}\"."
                                )
                            }
                        }
                        items(visible, key = { it.item.id.value }) { entry ->
                            InventoryCard(entry, onClick = { onEvent(InventoryEvent.OnItemClicked(entry.item.id.value)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(entry: InventoryEntry, onClick: () -> Unit) {
    val item = entry.item
    ItemCard(
        category = item.category.label,
        title = item.title,
        date = "Encontrado: ${item.foundAt.toLocalDate().formatShort()}",
        image = { ItemPhoto(item) },
        location = item.foundZone.label,
        // "Resaltar por vencer": el chip va arriba de la tarjeta en naranja o rojo.
        status = { EntryChip(entry) },
        variant = AppCardVariant.Filled,
        dimmed = !entry.inCustody,
        onClick = onClick
    )
}

private val InventoryEntry.inCustody: Boolean
    get() = item.status == ItemStatus.DISPONIBLE || item.status == ItemStatus.RECLAMADO

/** Retención si requiere atención; si no, el estado del objeto. */
@Composable
internal fun EntryChip(entry: InventoryEntry) {
    when (entry.retention) {
        RetentionState.VENCIDO -> StatusChip("Vencido", StatusTone.Error)
        RetentionState.POR_VENCER -> StatusChip("Por vencer", StatusTone.Warning)
        RetentionState.VIGENTE -> StatusChip(entry.item.status.label, entry.item.status.tone())
    }
}

internal fun ItemStatus.tone(): StatusTone = when (this) {
    ItemStatus.DISPONIBLE -> StatusTone.Success
    ItemStatus.RECLAMADO -> StatusTone.Info
    ItemStatus.ENTREGADO -> StatusTone.Neutral
    ItemStatus.DESCARTADO, ItemStatus.DONADO -> StatusTone.Neutral
}

/** Primera foto o el ícono de la categoría (el seed no trae fotos). */
@Composable
internal fun ItemPhoto(item: FoundItem, modifier: Modifier = Modifier.fillMaxSize()) {
    val colors = UcbEncuentraTheme.colors
    val photo = item.photoUrls.firstOrNull()
    Box(
        modifier.clip(UcbEncuentraTheme.shapes.small).background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            AsyncImage(model = photo, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(item.category.icon(), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}
