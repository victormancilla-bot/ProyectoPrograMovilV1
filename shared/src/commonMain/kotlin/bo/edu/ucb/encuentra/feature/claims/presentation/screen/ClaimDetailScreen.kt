package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Lock
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
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerReview
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDecision
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailEffect
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailState
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeader
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Detalle de reclamo para el staff (V01): preguntas de control con lo que respondió el estudiante, la respuesta
 * esperada y la sugerencia de la app. La decisión (aprobar / rechazar) es del staff. V03 le pone el diseño final.
 */
@Composable
fun ClaimDetailScreen(
    claimId: String,
    viewModel: ClaimDetailViewModel = koinViewModel { parametersOf(claimId) },
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ClaimDetailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    ClaimDetailContent(state, viewModel::handleEvent)
}

@Composable
private fun ClaimDetailContent(state: ClaimDetailState, onEvent: (ClaimDetailEvent) -> Unit) {
    val review = state.review
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = review?.let { "Reclamo #${it.claim.number}" } ?: "Detalle de reclamo",
                subtitle = review?.item?.title,
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { onEvent(ClaimDetailEvent.OnBack) },
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    review != null -> ReviewBody(review, state, onEvent)
                    state.loading -> LoadingState(message = "Cargando reclamo...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(ClaimDetailEvent.OnRetry) })
                    else -> EmptyState(title = "Reclamo no disponible", message = "No existe o no tienes permiso para verlo.")
                }
            }
        }
    }

    state.confirming?.let { decision -> DecisionDialog(decision, state.note, onEvent) }
}

@Composable
private fun ReviewBody(review: ClaimReview, state: ClaimDetailState, onEvent: (ClaimDetailEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppOverline("Reclamado por", modifier = Modifier.weight(1f))
                    StatusChip(review.claim.status.label, review.claim.status.tone())
                }
                AppTitle(review.claimantName ?: review.claim.claimantId)
                AppBody("Objeto: ${review.item.title} · ${review.item.status.label}")
                AppBody("Intentos fallidos: ${review.claim.failedAttempts} de ${Claim.MAX_ANSWER_ATTEMPTS}")
            }
        }

        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.large)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = UcbEncuentraTheme.colors.primary, modifier = Modifier.size(18.dp))
                    AppOverline("  Preguntas de control (privado)", color = UcbEncuentraTheme.colors.primary)
                }
                review.answers.forEachIndexed { index, answer -> AnswerRow(index + 1, answer) }
                AppLabel(
                    "La sugerencia compara palabras clave con la respuesta esperada. La decisión es tuya: " +
                        "si tienes dudas, pide al estudiante que muestre su carnet o más detalles al recoger."
                )
            }
        }

        if (review.canApprove || review.canReject) {
            if (review.canApprove) {
                PrimaryButton(
                    text = "Aprobar reclamo",
                    onClick = { onEvent(ClaimDetailEvent.OnDecide(ClaimDecision.APPROVE)) },
                    enabled = !state.working,
                    size = AppButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppLabel("Se podrá aprobar cuando el estudiante responda las preguntas de control.")
            }
            SecondaryButton(
                text = "Rechazar reclamo",
                onClick = { onEvent(ClaimDetailEvent.OnDecide(ClaimDecision.REJECT)) },
                enabled = !state.working,
                size = AppButtonSize.Large,
                color = UcbEncuentraTheme.colors.error,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppBody("Este reclamo ya está ${review.claim.status.label.lowercase()}: no hay decisiones pendientes.")
        }
        state.actionError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }
        Spacer(Modifier.height(spacing.medium))
    }
}

@Composable
private fun AnswerRow(position: Int, answer: AnswerReview) {
    val colors = UcbEncuentraTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceVariant, UcbEncuentraTheme.shapes.medium).padding(UcbEncuentraTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLabel("$position. ${answer.question}", color = colors.onSurface, modifier = Modifier.weight(1f))
            answer.verdict?.let { StatusChip(it.label, it.tone()) }
        }
        AppBody("Respondió: ${answer.answer ?: "— (aún no responde)"}", color = colors.onSurface)
        AppBody("Esperada: ${answer.expected?.takeIf { it.isNotBlank() } ?: "— (no se registró)"}")
    }
}

@Composable
private fun DecisionDialog(decision: ClaimDecision, note: String, onEvent: (ClaimDetailEvent) -> Unit) {
    val approve = decision == ClaimDecision.APPROVE
    AlertDialog(
        onDismissRequest = { onEvent(ClaimDetailEvent.OnDismiss) },
        title = { AppTitle(if (approve) "¿Aprobar el reclamo?" else "¿Rechazar el reclamo?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
                AppBody(
                    if (approve) {
                        "El objeto quedará reservado para este estudiante hasta la entrega."
                    } else {
                        "La coincidencia se descarta y el objeto vuelve a estar disponible si nadie más lo reclama."
                    }
                )
                AppTextField(
                    value = note,
                    onValueChange = { onEvent(ClaimDetailEvent.OnNoteChanged(it)) },
                    placeholder = "Motivo (opcional, queda en la auditoría)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(ClaimDetailEvent.OnConfirm) }) {
                AppLabel(
                    if (approve) "Aprobar" else "Rechazar",
                    color = if (approve) UcbEncuentraTheme.colors.primary else UcbEncuentraTheme.colors.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ClaimDetailEvent.OnDismiss) }) {
                AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
            }
        }
    )
}

private fun AnswerVerdict.tone(): StatusTone = when (this) {
    AnswerVerdict.COINCIDE -> StatusTone.Success
    AnswerVerdict.NO_COINCIDE -> StatusTone.Error
    AnswerVerdict.SIN_REFERENCIA -> StatusTone.Neutral
}
