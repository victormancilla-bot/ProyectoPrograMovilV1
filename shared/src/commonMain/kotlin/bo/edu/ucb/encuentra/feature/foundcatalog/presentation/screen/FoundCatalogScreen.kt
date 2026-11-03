package bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogEffect
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogEvent
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogState
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

private const val ALL = "Todas"

/**
 * L07 · Objetos encontrados (pestaña Buscar del estudiante). El PDF no tiene esta pantalla: se arma con el DS
 * (ItemCard como en Resultados de Matching). Solo categoría, zona y fecha; la foto va difuminada.
 */
@Composable
fun FoundCatalogScreen(
    viewModel: FoundCatalogViewModel = koinViewModel(),
    onReport: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FoundCatalogEffect.NavigateToReport -> onReport()
            }
        }
    }

    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Objetos encontrados",
                subtitle = when (state.items.size) {
                    0 -> "En Objetos Perdidos"
                    1 -> "1 objeto en Objetos Perdidos"
                    else -> "${state.items.size} objetos en Objetos Perdidos"
                }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando objetos encontrados...")
                    state.error != null -> ErrorState(message = state.error!!)
                    else -> CatalogList(state, viewModel::handleEvent)
                }
            }
        }
    }
}

@Composable
private fun CatalogList(state: FoundCatalogState, onEvent: (FoundCatalogEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        item { PrivacyNote() }
        if (state.items.isNotEmpty()) {
            item { Filters(state, onEvent) }
        }
        when {
            state.items.isEmpty() -> item {
                EmptyState(
                    title = "No hay objetos por ahora",
                    message = "Cuando Objetos Perdidos registre algo, aparecerá aquí. Si perdiste algo, repórtalo y te avisamos.",
                    actionText = "Reportar objeto",
                    onAction = { onEvent(FoundCatalogEvent.OnReport) }
                )
            }
            state.visible.isEmpty() -> item {
                EmptyState(
                    title = "Sin resultados",
                    message = "No hay objetos con esos filtros.",
                    icon = Icons.Outlined.SearchOff,
                    actionText = "Quitar filtros",
                    onAction = { onEvent(FoundCatalogEvent.OnClearFilters) }
                )
            }
            else -> items(state.visible, key = { it.id }) { CatalogCard(it, onEvent) }
        }
    }
}

@Composable
private fun PrivacyNote() {
    val spacing = UcbEncuentraTheme.spacing
    AppCard(Modifier.fillMaxWidth(), variant = AppCardVariant.Filled) {
        Row(
            Modifier.padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = UcbEncuentraTheme.colors.primary)
            AppBody(
                "Para proteger a los dueños solo mostramos el tipo, la zona y la fecha. " +
                    "Si algo es tuyo, repórtalo: te avisamos con la coincidencia y lo verificas con preguntas de control."
            )
        }
    }
}

@Composable
private fun Filters(state: FoundCatalogState, onEvent: (FoundCatalogEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppOverline("Categoría", modifier = Modifier.weight(1f))
            if (state.hasFilters) {
                TextButton(onClick = { onEvent(FoundCatalogEvent.OnClearFilters) }) {
                    AppLabel("Quitar filtros", color = UcbEncuentraTheme.colors.primary)
                }
            }
        }
        AppChipGroup(
            options = listOf(ALL) + state.categories.map { it.label },
            selected = state.filter.category?.label ?: ALL,
            onSelect = { label -> onEvent(FoundCatalogEvent.OnCategorySelected(state.categories.firstOrNull { it.label == label })) }
        )
        AppOverline("Zona")
        AppChipGroup(
            options = listOf(ALL) + state.zones.map { it.label },
            selected = state.filter.zone?.label ?: ALL,
            onSelect = { label -> onEvent(FoundCatalogEvent.OnZoneSelected(state.zones.firstOrNull { it.label == label })) }
        )
    }
}

@Composable
private fun CatalogCard(item: CatalogItem, onEvent: (FoundCatalogEvent) -> Unit) {
    ItemCard(
        category = item.category.label,
        title = item.subcategory.label,
        date = "Encontrado: ${item.foundAt.toLocalDate().formatShort()}",
        location = item.zone.label,
        image = { BlurredPhoto(item) },
        actionText = "¿Es tuyo? Repórtalo",
        onAction = { onEvent(FoundCatalogEvent.OnReport) },
        variant = AppCardVariant.Filled
    )
}

/**
 * La foto va difuminada: deja ver qué tipo de objeto es, pero no stickers, nombres ni documentos que el dueño
 * usaría para responder las preguntas de control (`Modifier.blur` funciona desde Android 12). Sin foto, el ícono.
 */
@Composable
private fun BlurredPhoto(item: CatalogItem) {
    val colors = UcbEncuentraTheme.colors
    Box(Modifier.fillMaxSize().background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
        if (item.photoUrl != null) {
            AsyncImage(
                model = item.photoUrl,
                contentDescription = "Foto difuminada de ${item.subcategory.label}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(16.dp)
            )
        } else {
            Icon(item.category.icon(), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}
