package bo.edu.ucb.encuentra.feature.dashboard.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.ClickableField
import bo.edu.ucb.encuentra.core.presentation.PastDatePickerDialog
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.CategoryRecovery
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.Metrics
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.MetricsPeriod
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsEffect
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsState
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsViewModel
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.StatCard
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
import kotlin.math.roundToInt

/**
 * D02 · Estadísticas (staff). Sin mockup en el PDF: cifras destacadas con StatCard y un gráfico de barras simple
 * (una sola serie: % devuelto por categoría, en el color primario, con el dato escrito en cada fila).
 */
@Composable
fun MetricsScreen(
    viewModel: MetricsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MetricsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Estadísticas",
                subtitle = "Objetos encontrados y su destino",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { viewModel.handleEvent(MetricsEvent.OnBack) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Calculando estadísticas...")
                    state.error != null -> ErrorState(message = state.error!!)
                    else -> MetricsBody(state, viewModel::handleEvent)
                }
            }
        }
    }
}

@Composable
private fun MetricsBody(state: MetricsState, onEvent: (MetricsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val metrics = state.metrics
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        PeriodFilter(state, onEvent)
        if (metrics.isEmpty) {
            EmptyState(
                title = "Sin objetos en este rango",
                message = "No se registraron objetos encontrados entre esas fechas.",
                icon = Icons.Outlined.CalendarMonth
            )
        } else {
            Headline(metrics)
            CategoryChart(metrics.byCategory)
            Destinations(metrics)
        }
    }
}

@Composable
private fun PeriodFilter(state: MetricsState, onEvent: (MetricsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    var picking by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        AppOverline("Encontrados en")
        AppChipGroup(
            options = MetricsPeriod.entries.map { it.label },
            selected = state.period.label,
            onSelect = { label -> MetricsPeriod.entries.firstOrNull { it.label == label }?.let { onEvent(MetricsEvent.OnPeriodSelected(it)) } }
        )
        if (state.period == MetricsPeriod.PERSONALIZADO) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                ClickableField(
                    value = state.custom.from?.formatShort() ?: "Elegir",
                    label = "Desde",
                    onClick = { picking = "from" },
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                ClickableField(
                    value = state.custom.to?.formatShort() ?: "Elegir",
                    label = "Hasta",
                    onClick = { picking = "to" },
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            val range = state.range
            AppLabel(
                if (range.from == null) "Todos los registros" else "Del ${range.from.formatShort()} al ${range.to?.formatShort()}",
                color = UcbEncuentraTheme.colors.onSurfaceVariant
            )
        }
    }
    when (picking) {
        "from" -> PastDatePickerDialog(state.custom.from, { onEvent(MetricsEvent.OnFromPicked(it)); picking = null }, { picking = null })
        "to" -> PastDatePickerDialog(state.custom.to, { onEvent(MetricsEvent.OnToPicked(it)); picking = null }, { picking = null })
    }
}

@Composable
private fun Headline(metrics: Metrics) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            StatCard(
                value = metrics.found.toString(), label = "Objetos encontrados", icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = metrics.recoveryPercent?.let { "$it %" } ?: "—",
                label = "Volvieron a su dueño (${metrics.returned})", icon = Icons.Outlined.TaskAlt,
                valueColor = UcbEncuentraTheme.colors.success, modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            StatCard(
                value = days(metrics.avgDaysToReturn), label = "Promedio hasta la entrega", icon = Icons.Outlined.Timelapse,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = days(metrics.avgRetentionDays), label = "Retención promedio (salidos)", icon = Icons.Outlined.Schedule,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Barras horizontales: un solo color (es una sola serie), extremos redondeados y el dato escrito en la fila. */
@Composable
private fun CategoryChart(rows: List<CategoryRecovery>) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            AppTitle("Recuperación por categoría")
            AppLabel("Porcentaje de objetos encontrados que volvieron a su dueño.", color = colors.onSurfaceVariant)
            rows.forEach { row ->
                Column(
                    Modifier.semantics {
                        contentDescription = "${row.category.label}: ${row.percent} por ciento, ${row.returned} de ${row.found} devueltos"
                    },
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppBody(row.category.label, color = colors.onSurface, modifier = Modifier.weight(1f))
                        AppLabel("${row.percent} %", color = colors.onSurface)
                    }
                    // Pista recesiva + barra en el color primario; el ancho es el porcentaje.
                    Box(Modifier.fillMaxWidth().height(BAR_HEIGHT).background(colors.surfaceContainer, BAR_SHAPE)) {
                        if (row.returned > 0) {
                            Box(Modifier.fillMaxWidth(row.rate.toFloat()).height(BAR_HEIGHT).background(colors.primary, BAR_SHAPE))
                        }
                    }
                    AppLabel("${row.returned} de ${row.found} devueltos", color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Destinations(metrics: Metrics) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            AppTitle("Destino de los objetos")
            listOf(
                "Devueltos a su dueño" to metrics.returned,
                "Donados" to metrics.donated,
                "Descartados" to metrics.discarded,
                "Siguen en custodia" to metrics.inCustody
            ).forEach { (label, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppBody(label, color = colors.onSurface, modifier = Modifier.weight(1f))
                    AppTitle(count.toString())
                }
            }
            metrics.avgDaysInCustodyNow?.let {
                AppLabel("Los que siguen en custodia llevan en promedio ${days(it)}.", color = colors.onSurfaceVariant)
            }
        }
    }
}

/** "3,5 días" (un decimal por debajo de 10; entero desde 10) o "—" si no hay datos. */
internal fun days(value: Double?): String {
    if (value == null) return "—"
    val text = if (value < 10) ((value * 10).roundToInt() / 10.0).toString().replace('.', ',').removeSuffix(",0") else value.roundToInt().toString()
    return if (text == "1") "1 día" else "$text días"
}

private val BAR_HEIGHT = 12.dp
private val BAR_SHAPE = RoundedCornerShape(4.dp)
