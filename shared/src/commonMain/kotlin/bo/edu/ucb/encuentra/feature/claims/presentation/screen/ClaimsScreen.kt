package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.presentation.formatRelative
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEffect
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsState
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
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
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/** Figma "Verificación de Reclamos" (V03): reclamos por revisar, primero los que ya respondieron las preguntas. */
@Composable
fun ClaimsScreen(
    viewModel: ClaimsViewModel = koinViewModel(),
    onOpenClaim: (claimId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ClaimsEffect.NavigateToDetail -> onOpenClaim(effect.claimId)
            }
        }
    }

    ClaimsContent(state, viewModel::handleEvent)
}

@Composable
private fun ClaimsContent(state: ClaimsState, onEvent: (ClaimsEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(title = "UCB Encuentra", centerTitle = true)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando reclamos...")
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(ClaimsEvent.OnRetry) })
                    else -> LazyColumn(
                        contentPadding = PaddingValues(spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.large)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                                AppTitle("Verificación de Reclamos", color = UcbEncuentraTheme.colors.tertiary)
                                AppBody("Revisa y gestiona las solicitudes de reclamo de objetos perdidos.")
                            }
                        }
                        if (state.claims.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "Sin reclamos por revisar",
                                    message = "Cuando un estudiante reclame un objeto, aparecerá aquí."
                                )
                            }
                        }
                        items(state.claims, key = { it.claim.id.value }) { review ->
                            val id = review.claim.id.value
                            ClaimCard(
                                review = review,
                                now = state.now,
                                expanded = id in state.expanded,
                                onToggle = { onEvent(ClaimsEvent.OnToggle(id)) },
                                onReview = { onEvent(ClaimsEvent.OnClaimClicked(id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimCard(review: ClaimReview, now: Instant?, expanded: Boolean, onToggle: () -> Unit, onReview: () -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppCard(Modifier.fillMaxWidth(), onClick = onToggle) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                ItemThumbnail(review)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppOverline("Reclamo #${review.claim.number}", color = UcbEncuentraTheme.colors.onSurface)
                    AppTitle(review.item.title)
                    AppBody("Reclamado por: ${review.claimantName ?: review.claim.claimantId}")
                    now?.let { ElapsedChip(formatRelative(review.claim.createdAt, it)) }
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Ocultar resumen" else "Ver resumen"
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    HorizontalDivider(color = UcbEncuentraTheme.colors.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLabel("Estado", modifier = Modifier.weight(1f))
                        StatusChip(review.claim.status.label, review.claim.status.tone())
                    }
                    AppBody(answersSummary(review))
                    if (review.claim.failedAttempts > 0) {
                        AppBody("Intentos fallidos: ${review.claim.failedAttempts} de ${Claim.MAX_ANSWER_ATTEMPTS}")
                    }
                    review.matchScore?.let { AppBody("Parecido con su reporte: $it %") }
                    PrimaryButton(
                        text = "Revisar reclamo",
                        onClick = onReview,
                        size = AppButtonSize.Regular,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun answersSummary(review: ClaimReview): String {
    val answered = review.answers.count { it.answer != null }
    return if (answered == 0) {
        "Aún no respondió las preguntas de control."
    } else {
        "Sugerencia: ${review.matchingAnswers} de ${review.answers.size} respuestas coinciden."
    }
}

/** Primera foto del objeto o el ícono de su categoría (el seed no trae fotos). */
@Composable
internal fun ItemThumbnail(review: ClaimReview, size: Int = 64) {
    val colors = UcbEncuentraTheme.colors
    val shape = UcbEncuentraTheme.shapes.small
    val photo = review.item.photoUrls.firstOrNull()
    Box(
        Modifier.size(size.dp).clip(shape).background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            AsyncImage(model = photo, contentDescription = review.item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(review.item.category.icon(), contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

/** Chip "Hace 2 horas" del mockup. */
@Composable
internal fun ElapsedChip(text: String) {
    val colors = UcbEncuentraTheme.colors
    Row(
        Modifier.background(colors.surfaceVariant, UcbEncuentraTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
        AppLabel(text)
    }
}

internal fun ClaimStatus.tone(): StatusTone = when (this) {
    ClaimStatus.PENDIENTE -> StatusTone.Warning
    ClaimStatus.EN_VERIFICACION -> StatusTone.Info
    ClaimStatus.APROBADO, ClaimStatus.ENTREGADO -> StatusTone.Success
    ClaimStatus.RECHAZADO -> StatusTone.Error
}
