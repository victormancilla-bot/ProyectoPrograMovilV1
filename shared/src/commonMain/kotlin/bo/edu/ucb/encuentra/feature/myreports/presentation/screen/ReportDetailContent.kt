package bo.edu.ucb.encuentra.feature.myreports.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
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
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.presentation.formatDateRange
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportDetail
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsEvent
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsState
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Detalle de un reporte (L05): sus datos, sus coincidencias con su % y "Cerrar reporte" si sigue buscando. */
@Composable
internal fun ReportDetailContent(state: MyReportsState, onEvent: (MyReportsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Detalle del reporte",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver a mis reportes",
                onNavigationClick = { onEvent(MyReportsEvent.OnBackFromDetail) },
                centerTitle = true
            )
            val detail = state.detail
            if (detail == null) {
                LoadingState()
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screenPadding, vertical = spacing.large),
                    verticalArrangement = Arrangement.spacedBy(spacing.large)
                ) {
                    ReportSummary(detail)
                    ReportMatches(detail, onEvent)
                    if (state.canClose) {
                        SecondaryButton(
                            text = if (state.closing) "Cerrando..." else "Cerrar reporte",
                            onClick = { onEvent(MyReportsEvent.OnCloseReportClicked) },
                            enabled = !state.closing,
                            color = UcbEncuentraTheme.colors.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    state.closeError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }
                }
            }
        }
    }

    if (state.showCloseDialog) CloseReportDialog(onEvent)
}

@Composable
private fun ReportSummary(detail: ReportDetail) {
    val report = detail.report
    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
                AppOverline("${report.category.label} / ${report.subcategory.label}")
                AppTitle(report.title)
            }
            report.status.statusChip()?.invoke()
        }
        DetailRow(
            icon = Icons.Filled.Palette,
            label = "Color y marca",
            value = listOfNotNull(report.color, report.brand).joinToString(" · ").ifEmpty { "Sin datos" }
        )
        if (report.description.isNotBlank()) DetailRow(Icons.Filled.Description, "Características", report.description)
        DetailRow(
            icon = DsIcons.Calendar,
            label = "Cuándo",
            value = formatDateRange(report.lostFrom.toLocalDate(), report.lostTo.toLocalDate())
        )
        DetailRow(DsIcons.Location, "Dónde", report.locationText)
        report.contactPhone?.let { DetailRow(Icons.Filled.Phone, "Teléfono de contacto", it) }
        report.photoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = "Foto de referencia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(UcbEncuentraTheme.shapes.medium)
            )
        }
        AppLabel("Reportado el ${report.createdAt.toLocalDate().formatShort()}")
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
        Icon(icon, contentDescription = null, tint = UcbEncuentraTheme.colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column {
            AppLabel(label)
            AppBody(value, color = UcbEncuentraTheme.colors.onSurface)
        }
    }
}

@Composable
private fun ReportMatches(detail: ReportDetail, onEvent: (MyReportsEvent) -> Unit) {
    val matches = detail.matches
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.large)) {
        AppOverline("Coincidencias (${matches.size})", color = UcbEncuentraTheme.colors.onSurface)
        if (matches.isEmpty()) {
            AppBody("Todavía no hay objetos parecidos. Te avisaremos en Alertas apenas aparezca uno.")
        }
        matches.forEachIndexed { index, match ->
            ItemCard(
                category = "#${index + 1} · ${match.itemCategory.label}",
                title = match.itemTitle,
                date = "Encontrado: ${match.foundAt.toLocalDate().formatShort()}" +
                    if (match.status == MatchStatus.CONFIRMADO) " · Confirmado" else "",
                score = match.score,
                variant = AppCardVariant.Filled,
                onClick = { onEvent(MyReportsEvent.OnMatchClicked(match.matchId)) }
            )
        }
        if (matches.isNotEmpty()) {
            SecondaryButton(
                text = "Ver en Posibles coincidencias",
                onClick = { onEvent(MyReportsEvent.OnSeeAllMatches) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Confirmación con el motivo: "lo encontré" (RECUPERADO) o "ya no lo busco" (CERRADO). */
@Composable
private fun CloseReportDialog(onEvent: (MyReportsEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    AlertDialog(
        onDismissRequest = { onEvent(MyReportsEvent.OnDismissClose) },
        title = { AppTitle("¿Cerrar este reporte?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
                AppBody("Dejaremos de buscar coincidencias para este objeto. ¿Por qué lo cierras?")
                CloseReason.entries.forEach { reason ->
                    Box(Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEvent(MyReportsEvent.OnConfirmClose(reason)) }) {
                            AppLabel(reason.label, color = if (reason == CloseReason.FOUND_IT) colors.success else colors.error)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onEvent(MyReportsEvent.OnDismissClose) }) {
                AppLabel("Cancelar", color = colors.primary)
            }
        }
    )
}
