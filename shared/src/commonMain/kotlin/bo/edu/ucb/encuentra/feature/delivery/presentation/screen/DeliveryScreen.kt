package bo.edu.ucb.encuentra.feature.delivery.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.ClickableField
import bo.edu.ucb.encuentra.core.presentation.FutureDatePickerDialog
import bo.edu.ucb.encuentra.core.presentation.TimePickerDialog
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.formatShort
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.ItemThumbnail
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.tone
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryEffect
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryEvent
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryState
import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryViewModel
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.selection.CheckRow
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes

/** Duración de la cita que se muestra ("14:30 - 15:00", como el mockup). */
private val SLOT = 30.minutes

/** Figma "Confirmación de Entrega" (V04): listo para entrega, fecha programada y verificación física. */
@Composable
fun DeliveryScreen(
    claimId: String,
    viewModel: DeliveryViewModel = koinViewModel { parametersOf(claimId) },
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DeliveryEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    DeliveryContent(state, viewModel::handleEvent)
}

@Composable
private fun DeliveryContent(state: DeliveryState, onEvent: (DeliveryEvent) -> Unit) {
    val review = state.review
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Confirmación de Entrega",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { onEvent(DeliveryEvent.OnBack) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    review != null -> DeliveryBody(review, state, onEvent)
                    state.loading -> LoadingState(message = "Cargando entrega...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(DeliveryEvent.OnRetry) })
                    else -> EmptyState(title = "Reclamo no disponible", message = "No existe o no tienes permiso para verlo.")
                }
            }
        }
    }
}

@Composable
private fun DeliveryBody(review: ClaimReview, state: DeliveryState, onEvent: (DeliveryEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            SummaryCard(review, state)
            if (state.readyForDelivery) {
                ScheduleCard(state, onEvent)
                PhysicalCheckCard(state, onEvent)
            } else {
                StatusNote(review, state)
            }
            state.actionError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }
        }
        // Botón fijo abajo, como en el mockup.
        Box(Modifier.padding(spacing.screenPadding)) {
            if (state.readyForDelivery) {
                PrimaryButton(
                    text = "Confirmar entrega",
                    onClick = { onEvent(DeliveryEvent.OnConfirm) },
                    enabled = state.canConfirm,
                    size = AppButtonSize.Large,
                    leadingIcon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SecondaryButton(
                    text = "Volver",
                    onClick = { onEvent(DeliveryEvent.OnBack) },
                    size = AppButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(UcbEncuentraTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
            content()
        }
    }
}

/** "Listo para entrega": objeto, reclamado por, aprobado por y fecha programada. */
@Composable
private fun SummaryCard(review: ClaimReview, state: DeliveryState) {
    val spacing = UcbEncuentraTheme.spacing
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTitle(if (state.delivered) "Entregado" else "Listo para entrega", modifier = Modifier.weight(1f))
            StatusChip(review.claim.status.label.uppercase(), review.claim.status.tone())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap), verticalAlignment = Alignment.CenterVertically) {
            ItemThumbnail(review, size = 56)
            Column(Modifier.weight(1f)) {
                AppBody(review.item.title, color = UcbEncuentraTheme.colors.onSurface)
                AppLabel("Ref: Reclamo #${review.claim.number}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
            Person("Reclamado por", review.claimantName ?: review.claim.claimantId, "Estudiante", Modifier.weight(1f))
            Person("Aprobado por", review.reviewerName ?: "—", "Staff", Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppOverline("Fecha y hora programada")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(DsIcons.Calendar, contentDescription = null, tint = UcbEncuentraTheme.colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                AppBody(scheduleText(review), color = UcbEncuentraTheme.colors.onSurface)
            }
        }
    }
}

@Composable
private fun Person(label: String, name: String, role: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppOverline(label)
        AppBody(name, color = UcbEncuentraTheme.colors.onSurface)
        AppLabel(role)
    }
}

private fun scheduleText(review: ClaimReview): String {
    val at = review.claim.scheduledDeliveryAt ?: return "Sin programar"
    val zone = TimeZone.currentSystemDefault()
    val start = at.toLocalDateTime(zone)
    val end = (at + SLOT).toLocalDateTime(zone)
    return "${start.date.formatShort()}, ${start.time.formatHm()} - ${end.time.formatHm()}"
}

/** Elegir o cambiar la cita. Programar es opcional: el estudiante también puede pasar sin cita. */
@Composable
private fun ScheduleCard(state: DeliveryState, onEvent: (DeliveryEvent) -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    Card {
        AppOverline("Programar entrega")
        Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
            ClickableField(
                value = state.date?.formatShort().orEmpty(),
                label = "Fecha",
                onClick = { picker = "date" },
                icon = DsIcons.Calendar,
                // Más ancho que la hora: "12 nov 2026" no entra en la mitad.
                modifier = Modifier.weight(1.5f)
            )
            ClickableField(
                value = state.time?.formatHm().orEmpty(),
                label = "Hora",
                onClick = { picker = "time" },
                icon = Icons.Outlined.Schedule,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryButton(
            text = "Guardar fecha y avisar al estudiante",
            onClick = { onEvent(DeliveryEvent.OnSchedule) },
            enabled = state.canSchedule,
            modifier = Modifier.fillMaxWidth()
        )
        state.info?.let { AppLabel(it, color = UcbEncuentraTheme.colors.success) }
    }
    when (picker) {
        "date" -> FutureDatePickerDialog(
            initial = state.date,
            onPick = { onEvent(DeliveryEvent.OnDatePicked(it)) },
            onDismiss = { picker = null }
        )
        "time" -> TimePickerDialog(
            initial = state.time ?: LocalTime(DEFAULT_HOUR, 0),
            onPick = { onEvent(DeliveryEvent.OnTimePicked(it)) },
            onDismiss = { picker = null }
        )
    }
}

private const val DEFAULT_HOUR = 10

/** "Verificación física": las dos casillas son obligatorias para confirmar. */
@Composable
private fun PhysicalCheckCard(state: DeliveryState, onEvent: (DeliveryEvent) -> Unit) {
    Card {
        AppOverline("Verificación física")
        CheckRow(
            text = "Confirmo que he verificado la identidad del estudiante usando su Carnet Universitario físico y coincide con los datos del sistema.",
            checked = state.checks.identityVerified,
            onCheckedChange = { onEvent(DeliveryEvent.OnIdentityVerified(it)) },
            enabled = !state.working
        )
        CheckRow(
            text = "El estudiante ha recibido el objeto listado arriba en las condiciones descritas originalmente.",
            checked = state.checks.itemReceived,
            onCheckedChange = { onEvent(DeliveryEvent.OnItemReceived(it)) },
            enabled = !state.working
        )
        if (!state.checks.complete) AppLabel("Marca ambas casillas para confirmar la entrega.")
    }
}

/** Reclamo ya entregado (o que todavía no está aprobado). */
@Composable
private fun StatusNote(review: ClaimReview, state: DeliveryState) {
    val colors = UcbEncuentraTheme.colors
    val text = if (state.delivered) {
        val at = review.claim.deliveredAt?.toLocalDateTime(TimeZone.currentSystemDefault())
        "Objeto entregado" + (at?.let { " el ${it.date.formatShort()} a las ${it.time.formatHm()}" } ?: "") +
            ". Quedó registrado en la auditoría y avisamos al estudiante."
    } else {
        "Este reclamo está ${review.claim.status.label.lowercase()}: solo se entregan reclamos aprobados."
    }
    Row(
        Modifier.fillMaxWidth().background(colors.secondary, UcbEncuentraTheme.shapes.large).padding(UcbEncuentraTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.onSecondary)
        AppBody(text, color = colors.onSecondary)
    }
    Spacer(Modifier.height(UcbEncuentraTheme.spacing.medium))
}
