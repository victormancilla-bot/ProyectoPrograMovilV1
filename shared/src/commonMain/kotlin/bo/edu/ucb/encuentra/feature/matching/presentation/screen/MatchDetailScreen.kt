package bo.edu.ucb.encuentra.feature.matching.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.presentation.formatShortRange
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.core.presentation.swatchFor
import bo.edu.ucb.encuentra.core.presentation.toLocalDate
import bo.edu.ucb.encuentra.feature.matching.domain.model.ComparisonVerdict
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchDetail
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailEffect
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailEvent
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailState
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.badges.ScoreBadge
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val NOT_GIVEN = "No indicado"

/** Figma "Detalle de Match" (M05): lo que reportaste vs lo que se encontró, con ✔ / ?. */
@Composable
fun MatchDetailScreen(
    matchId: String,
    viewModel: MatchDetailViewModel = koinViewModel { parametersOf(matchId) },
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MatchDetailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    MatchDetailContent(state = state, onEvent = viewModel::handleEvent)
}

@Composable
private fun MatchDetailContent(state: MatchDetailState, onEvent: (MatchDetailEvent) -> Unit) {
    val back = { onEvent(MatchDetailEvent.OnBack) }
    AppSurface(Modifier.fillMaxSize()) {
        val detail = state.detail
        when {
            detail != null -> Box(Modifier.fillMaxSize()) {
                DetailBody(detail, state, onEvent)
                // Franja fija detrás de la barra de estado: al hacer scroll sus íconos claros siguen legibles.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(UcbEncuentraTheme.colors.primary)
                )
            }
            else -> Column(Modifier.fillMaxSize()) {
                CurvedHeader(
                    title = "Detalle de hallazgo",
                    navigationIcon = DsIcons.ArrowBack,
                    navigationContentDescription = "Volver",
                    onNavigationClick = back
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.loading -> LoadingState(message = "Cargando coincidencia...")
                        state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(MatchDetailEvent.OnRetry) })
                        else -> EmptyState(
                            title = "Coincidencia no disponible",
                            message = "Esta coincidencia ya no existe o no corresponde a tus reportes.",
                            actionText = "Volver",
                            onAction = back
                        )
                    }
                }
            }
        }
    }

    if (state.confirmingReject) {
        AlertDialog(
            onDismissRequest = { onEvent(MatchDetailEvent.OnDismissReject) },
            title = { AppTitle("¿No es tuyo?") },
            text = { AppBody("Este objeto no volverá a aparecer como coincidencia de tu reporte.") },
            confirmButton = {
                TextButton(onClick = { onEvent(MatchDetailEvent.OnConfirmReject) }) {
                    AppLabel("No es mío", color = UcbEncuentraTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(MatchDetailEvent.OnDismissReject) }) {
                    AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun DetailBody(detail: MatchDetail, state: MatchDetailState, onEvent: (MatchDetailEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Hero(detail, onBack = { onEvent(MatchDetailEvent.OnBack) })
        Column(
            // La tarjeta del título se monta sobre el borde inferior de la foto, como en el mockup.
            modifier = Modifier.offset(y = (-28).dp).padding(horizontal = spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            TitleCard(detail)
            ComparisonCard(detail)
            ProtectedInfoNote()
            Actions(detail, state, onEvent)
        }
    }
}

/** Foto (o ícono de la categoría si no hay) con velo azul, botón atrás y el score en la esquina. */
@Composable
private fun Hero(detail: MatchDetail, onBack: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    Box(Modifier.fillMaxWidth().height(280.dp).background(colors.primary)) {
        val photo = detail.item.photoUrl
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = "Foto del objeto encontrado",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.6f)
            )
        } else {
            Icon(
                detail.item.subcategory.category.icon(),
                contentDescription = "Sin foto",
                tint = colors.onPrimary.copy(alpha = 0.25f),
                modifier = Modifier.align(Alignment.Center).size(120.dp)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.primary.copy(alpha = 0.9f))
            )
        )
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(DsIcons.ArrowBack, contentDescription = "Volver", tint = colors.onPrimary)
            }
            AppTitle("Detalle de hallazgo", color = colors.onPrimary)
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScoreBadge(score = detail.score, size = 72.dp)
            AppOverline("Parecido", color = colors.onPrimary)
        }
    }
}

@Composable
private fun TitleCard(detail: MatchDetail) {
    val found = detail.item.foundAt.toLocalDateTime(TimeZone.currentSystemDefault())
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(UcbEncuentraTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppHeading(detail.item.title)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(DsIcons.Calendar, contentDescription = null, tint = UcbEncuentraTheme.colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                AppBody("Encontrado: ${found.date.formatShort()}, ${found.time.formatHm()}")
            }
            if (detail.status == MatchStatus.CONFIRMADO) {
                AppLabel("Coincidencia confirmada por el staff", color = UcbEncuentraTheme.colors.success)
            }
        }
    }
}

/** Una fila: dato reportado · ✔/? · dato encontrado. */
private data class Row3(
    val reportedLabel: String,
    val reported: String,
    val foundLabel: String,
    val found: String,
    val verdict: ComparisonVerdict,
    val reportedColor: String? = null,
    val foundColor: String? = null
)

@Composable
private fun ComparisonCard(detail: MatchDetail) {
    val report = detail.report
    val item = detail.item
    val c = detail.comparison
    val rows = listOf(
        // Etiquetas cortas: en 390 dp de ancho "Marca observada" o "Lugar encontrado" se parten en dos líneas.
        Row3("Tipo", report.subcategory.label, "Tipo", item.subcategory.label, c.type),
        Row3("Marca", report.brand ?: NOT_GIVEN, "Marca", item.brand ?: NOT_GIVEN, c.brand),
        Row3(
            "Color", report.color ?: NOT_GIVEN, "Color", item.color ?: NOT_GIVEN, c.color,
            reportedColor = report.color, foundColor = item.color
        ),
        Row3("Lo perdiste en", report.zone.label, "Se encontró en", item.zone.label, c.zone),
        Row3(
            "Lo perdiste", formatShortRange(report.lostFrom.toLocalDate(), report.lostTo.toLocalDate()),
            "Se encontró", item.foundAt.toLocalDate().formatShort(), c.date
        )
    )
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(UcbEncuentraTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
        ) {
            AppOverline("Análisis de coincidencia", color = UcbEncuentraTheme.colors.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLabel("LO QUE REPORTASTE", Modifier.weight(1f))
                Spacer(Modifier.width(32.dp))
                AppLabel("LO QUE SE ENCONTRÓ", Modifier.weight(1f), textAlign = TextAlign.End)
            }
            rows.forEach { ComparisonRow(it) }
            AppLabel("✔ coincide · ? por verificar: lo confirma el staff al revisar tu reclamo.")
        }
    }
}

@Composable
private fun ComparisonRow(row: Row3) {
    val colors = UcbEncuentraTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Field(row.reportedLabel, row.reported, row.reportedColor, Alignment.Start, Modifier.weight(1f))
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (row.verdict == ComparisonVerdict.COINCIDE) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Coincide", tint = colors.success, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Outlined.HelpOutline, contentDescription = "Por verificar", tint = colors.neutral, modifier = Modifier.size(22.dp))
            }
        }
        Field(row.foundLabel, row.found, row.foundColor, Alignment.End, Modifier.weight(1f))
    }
}

@Composable
private fun Field(label: String, value: String, colorName: String?, align: Alignment.Horizontal, modifier: Modifier) {
    val textAlign = if (align == Alignment.End) TextAlign.End else TextAlign.Start
    Column(modifier, horizontalAlignment = align) {
        AppOverline(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            swatchFor(colorName)?.let { swatch ->
                Box(Modifier.size(12.dp).background(swatch.color, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            AppBody(value, color = UcbEncuentraTheme.colors.onSurface, textAlign = textAlign)
        }
    }
}

/** Vista limitada: en vez de la descripción del staff se explica qué datos se reservan para la verificación. */
@Composable
private fun ProtectedInfoNote() {
    val colors = UcbEncuentraTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant, UcbEncuentraTheme.shapes.large)
            .padding(UcbEncuentraTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppLabel("Información protegida", color = colors.onSurface)
            AppBody(
                "Para proteger al dueño, la descripción, el lugar exacto y dónde está guardado el objeto solo se " +
                    "muestran cuando el staff verifica tu reclamo."
            )
        }
    }
}

@Composable
private fun Actions(detail: MatchDetail, state: MatchDetailState, onEvent: (MatchDetailEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
        val claimNumber = state.claimNumber
        when {
            claimNumber != null -> InfoBox(
                "Reclamo #$claimNumber enviado. El staff revisará tus respuestas y te avisará cuándo recoger el objeto."
            )
            state.canAct -> {
                PrimaryButton(
                    text = "Esto es mío — verificar",
                    onClick = { onEvent(MatchDetailEvent.OnClaim) },
                    enabled = !state.working,
                    size = AppButtonSize.Large,
                    leadingIcon = Icons.Outlined.VerifiedUser,
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(
                    text = "No es lo mío, seguir buscando",
                    onClick = { onEvent(MatchDetailEvent.OnReject) },
                    enabled = !state.working,
                    size = AppButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            detail.status == MatchStatus.CONFIRMADO -> InfoBox("Esta coincidencia ya fue confirmada. Sigue tu reclamo en Mis reportes.")
            else -> InfoBox("Tu reporte ya no está activo, por eso no puedes reclamar este objeto.")
        }
        state.actionError?.let { AppLabel(it, color = colors.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        Spacer(Modifier.height(UcbEncuentraTheme.spacing.medium))
    }
}

@Composable
private fun InfoBox(text: String) {
    val colors = UcbEncuentraTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.secondary, UcbEncuentraTheme.shapes.large)
            .padding(UcbEncuentraTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.onSecondary, modifier = Modifier.size(20.dp))
        AppBody(text, color = colors.onSecondary)
    }
}
