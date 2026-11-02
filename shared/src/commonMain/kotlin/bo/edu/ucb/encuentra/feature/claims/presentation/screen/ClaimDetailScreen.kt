package bo.edu.ucb.encuentra.feature.claims.presentation.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.presentation.formatRelative
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerReview
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDecision
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailEffect
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailState
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.badges.ScoreBadge
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
import edu.ucb.ds.components.textfields.AppTextArea
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Detalle del reclamo para el staff (V03, no estaba en el PDF): quién reclama, fotos, reporte del estudiante vs
 * hallazgo, custodia, respuestas a las preguntas de control (V01) y la decisión con motivo obligatorio.
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

    state.confirming?.let { decision -> DecisionDialog(decision, state, onEvent) }
}

@Composable
private fun ReviewBody(review: ClaimReview, state: ClaimDetailState, onEvent: (ClaimDetailEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        ClaimantCard(review, state)
        PhotosCard(review)
        ComparisonCard(review)
        CustodyCard(review)
        AnswersCard(review)
        Decision(review, state, onEvent)
        Spacer(Modifier.height(spacing.medium))
    }
}

@Composable
private fun Section(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                icon?.let { Icon(it, contentDescription = null, tint = UcbEncuentraTheme.colors.primary, modifier = Modifier.size(18.dp)) }
                AppOverline(title, color = UcbEncuentraTheme.colors.primary)
            }
            content()
        }
    }
}

@Composable
private fun ClaimantCard(review: ClaimReview, state: ClaimDetailState) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(UcbEncuentraTheme.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppOverline("Reclamado por")
                AppTitle(review.claimantName ?: review.claim.claimantId)
                StatusChip(review.claim.status.label, review.claim.status.tone())
                state.now?.let { ElapsedChip(formatRelative(review.claim.createdAt, it)) }
                if (review.claim.failedAttempts > 0) {
                    AppLabel("Intentos fallidos: ${review.claim.failedAttempts} de ${Claim.MAX_ANSWER_ATTEMPTS}")
                }
            }
            // % de parecido de la coincidencia (M02), solo si el reclamo viene de su propio reporte.
            review.matchScore?.let { ScoreBadge(score = it, size = 56.dp) }
        }
    }
}

@Composable
private fun PhotosCard(review: ClaimReview) {
    val photos = review.item.photoUrls.map { it to "Hallazgo" } + listOfNotNull(review.report?.photoUrl?.let { it to "Reporte" })
    Section("Fotos", Icons.Outlined.PhotoLibrary) {
        if (photos.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap), verticalAlignment = Alignment.CenterVertically) {
                ItemThumbnail(review, size = 72)
                AppBody("Sin fotos registradas: compara con la descripción y las preguntas de control.", modifier = Modifier.weight(1f))
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
                photos.forEach { (url, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Foto del $label",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(120.dp).clip(UcbEncuentraTheme.shapes.medium)
                        )
                        AppLabel(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(review: ClaimReview) {
    val report = review.report
    Section("Reporte del estudiante vs Hallazgo") {
        if (report == null) {
            AppBody("Este reclamo no viene de un reporte del estudiante: decide con las preguntas de control.")
            return@Section
        }
        Row {
            AppLabel("REPORTÓ", Modifier.weight(1f))
            Spacer(Modifier.width(28.dp))
            AppLabel("SE ENCONTRÓ", Modifier.weight(1f), textAlign = TextAlign.End)
        }
        comparisonLines(report, review.item).forEach { line -> ComparisonRow(line) }
    }
}

@Composable
private fun ComparisonRow(line: ComparisonLine) {
    val colors = UcbEncuentraTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppOverline(line.label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppBody(line.reported, Modifier.weight(1f), color = colors.onSurface)
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                when (line.matches) {
                    true -> Icon(Icons.Filled.CheckCircle, contentDescription = "Coincide", tint = colors.success, modifier = Modifier.size(20.dp))
                    false -> Icon(Icons.Filled.Cancel, contentDescription = "No coincide", tint = colors.error, modifier = Modifier.size(20.dp))
                    null -> Unit
                }
            }
            AppBody(line.found, Modifier.weight(1f), color = colors.onSurface, textAlign = TextAlign.End)
        }
    }
}

/** Datos privados del hallazgo que el estudiante no ve (M05): dónde está guardado. */
@Composable
private fun CustodyCard(review: ClaimReview) {
    Section("Custodia (privado)", Icons.Outlined.Inventory2) {
        AppBody(
            "Guardado en: ${review.item.custodyPlace.label}" + review.item.custodyDetail.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
            color = UcbEncuentraTheme.colors.onSurface
        )
        AppBody("Estado del objeto: ${review.item.status.label}")
    }
}

@Composable
private fun AnswersCard(review: ClaimReview) {
    Section("Preguntas de control (privado)", Icons.Outlined.Lock) {
        review.answers.forEachIndexed { index, answer -> AnswerRow(index + 1, answer) }
        AppLabel(
            "La sugerencia compara palabras clave con la respuesta esperada. La decisión es tuya: " +
                "si tienes dudas, pide al estudiante su carnet o más detalles al recoger."
        )
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
private fun Decision(review: ClaimReview, state: ClaimDetailState, onEvent: (ClaimDetailEvent) -> Unit) {
    if (!review.canApprove && !review.canReject) {
        AppBody("Este reclamo ya está ${review.claim.status.label.lowercase()}: no hay decisiones pendientes.")
        return
    }
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
    state.actionError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }
}

@Composable
private fun DecisionDialog(decision: ClaimDecision, state: ClaimDetailState, onEvent: (ClaimDetailEvent) -> Unit) {
    val approve = decision == ClaimDecision.APPROVE
    val colors = UcbEncuentraTheme.colors
    AlertDialog(
        onDismissRequest = { onEvent(ClaimDetailEvent.OnDismiss) },
        title = { AppTitle(if (approve) "¿Aprobar el reclamo?" else "¿Rechazar el reclamo?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
                AppBody(
                    if (approve) {
                        "El objeto quedará reservado para este estudiante hasta la entrega. Le avisaremos."
                    } else {
                        "La coincidencia se descarta y el objeto vuelve a estar disponible si nadie más lo reclama. Le avisaremos."
                    }
                )
                AppTextArea(
                    value = state.note,
                    onValueChange = { onEvent(ClaimDetailEvent.OnNoteChanged(it)) },
                    label = "Motivo (obligatorio)",
                    placeholder = if (approve) "Ej. Describió el interior y mostró su carnet" else "Ej. No supo describir el colgante",
                    errorMessage = if (state.reasonMissing) "Escribe el motivo: queda en la auditoría." else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(ClaimDetailEvent.OnConfirm) }, enabled = state.canConfirm) {
                AppLabel(
                    if (approve) "Aprobar" else "Rechazar",
                    color = when {
                        !state.canConfirm -> colors.outline
                        approve -> colors.primary
                        else -> colors.error
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ClaimDetailEvent.OnDismiss) }) {
                AppLabel("Cancelar", color = colors.primary)
            }
        }
    )
}

private fun AnswerVerdict.tone(): StatusTone = when (this) {
    AnswerVerdict.COINCIDE -> StatusTone.Success
    AnswerVerdict.NO_COINCIDE -> StatusTone.Error
    AnswerVerdict.SIN_REFERENCIA -> StatusTone.Neutral
}
