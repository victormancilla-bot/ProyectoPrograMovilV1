package bo.edu.ucb.encuentra.feature.dashboard.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.presentation.ClickableField
import bo.edu.ucb.encuentra.core.presentation.PastDatePickerDialog
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.rememberFileSharer
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportKind
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportEffect
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportState
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportViewModel
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

private const val ALL = "Todos"
private const val CSV_MIME = "text/csv"

/** D03 · Exportar a CSV (staff). Sin mockup: filtros con chips del DS y un botón que abre "Compartir". */
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sharer = rememberFileSharer()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.Share -> viewModel.onShared(sharer.share(effect.file.name, CSV_MIME, effect.file.content), effect.file)
                ExportEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Exportar datos",
                subtitle = "CSV para Excel o Google Sheets",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { viewModel.handleEvent(ExportEvent.OnBack) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando datos...")
                    state.error != null -> ErrorState(message = state.error!!)
                    else -> ExportBody(state, viewModel::handleEvent)
                }
            }
        }
    }
}

@Composable
private fun ExportBody(state: ExportState, onEvent: (ExportEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    var picking by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            AppOverline("Qué exportar")
            AppChipGroup(
                options = ExportKind.entries.map { it.label },
                selected = state.kind.label,
                onSelect = { label -> ExportKind.entries.firstOrNull { it.label == label }?.let { onEvent(ExportEvent.OnKindSelected(it)) } }
            )
            AppLabel(
                if (state.kind == ExportKind.INVENTARIO) "Objetos encontrados con estado, retención y custodia."
                else "Quién hizo qué y cuándo (registro inalterable).",
                color = colors.onSurfaceVariant
            )

            AppOverline(if (state.kind == ExportKind.INVENTARIO) "Encontrados en" else "Fecha", modifier = Modifier.padding(top = spacing.medium))
            AppChipGroup(
                options = MetricsPeriod.entries.map { it.label },
                selected = state.period.label,
                onSelect = { label -> MetricsPeriod.entries.firstOrNull { it.label == label }?.let { onEvent(ExportEvent.OnPeriodSelected(it)) } }
            )
            if (state.period == MetricsPeriod.PERSONALIZADO) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                    ClickableField(state.custom.from?.formatShort() ?: "Elegir", "Desde", { picking = "from" }, Modifier.weight(1f), Icons.Outlined.CalendarMonth)
                    ClickableField(state.custom.to?.formatShort() ?: "Elegir", "Hasta", { picking = "to" }, Modifier.weight(1f), Icons.Outlined.CalendarMonth)
                }
            } else {
                val range = state.range
                AppLabel(
                    if (range.from == null) "Todos los registros" else "Del ${range.from.formatShort()} al ${range.to?.formatShort()}",
                    color = colors.onSurfaceVariant
                )
            }

            if (state.kind == ExportKind.INVENTARIO) {
                AppOverline("Estado", modifier = Modifier.padding(top = spacing.medium))
                AppChipGroup(
                    options = listOf(ALL) + ItemStatus.entries.map { it.label },
                    selected = state.status?.label ?: ALL,
                    onSelect = { label -> onEvent(ExportEvent.OnStatusSelected(ItemStatus.entries.firstOrNull { it.label == label })) }
                )
            }

            AppCard(Modifier.fillMaxWidth().padding(top = spacing.medium)) {
                Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    AppTitle(if (state.rows == 1) "1 fila" else "${state.rows} filas")
                    AppBody("Separado por punto y coma y con acentos en UTF-8: se abre directo en Excel. La exportación queda en la auditoría.")
                }
            }
        }
        Box(Modifier.padding(spacing.screenPadding)) {
            PrimaryButton(
                text = "Exportar y compartir",
                onClick = { onEvent(ExportEvent.OnExport) },
                enabled = !state.working && state.rows > 0,
                size = AppButtonSize.Large,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    when (picking) {
        "from" -> PastDatePickerDialog(state.custom.from, { onEvent(ExportEvent.OnFromPicked(it)); picking = null }, { picking = null })
        "to" -> PastDatePickerDialog(state.custom.to, { onEvent(ExportEvent.OnToPicked(it)); picking = null }, { picking = null })
    }
}
