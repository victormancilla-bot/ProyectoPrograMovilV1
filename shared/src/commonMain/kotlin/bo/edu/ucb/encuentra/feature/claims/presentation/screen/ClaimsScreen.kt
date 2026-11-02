package bo.edu.ucb.encuentra.feature.claims.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEffect
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsEvent
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
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

/**
 * Lista MÍNIMA de reclamos por revisar (V01), para llegar al detalle con las respuestas.
 * V03 la reemplaza por el diseño del mockup "Verificación de Reclamos".
 */
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

    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(title = "Reclamos", subtitle = "Revisa las solicitudes de reclamo", centerTitle = true)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando reclamos...")
                    state.error != null -> ErrorState(message = state.error.orEmpty(), onRetry = { viewModel.handleEvent(ClaimsEvent.OnRetry) })
                    state.claims.isEmpty() -> EmptyState(title = "Sin reclamos por revisar", message = "Cuando un estudiante reclame un objeto, aparecerá aquí.")
                    else -> LazyColumn(
                        contentPadding = PaddingValues(UcbEncuentraTheme.spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
                    ) {
                        items(state.claims, key = { it.claim.id.value }) { review ->
                            ClaimCard(review, onClick = { viewModel.handleEvent(ClaimsEvent.OnClaimClicked(review.claim.id.value)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimCard(review: ClaimReview, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), variant = AppCardVariant.Filled, onClick = onClick) {
        Column(Modifier.padding(UcbEncuentraTheme.spacing.large), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppOverline("Reclamo #${review.claim.number}", modifier = Modifier.weight(1f))
                StatusChip(review.claim.status.label, review.claim.status.tone())
            }
            AppTitle(review.item.title)
            AppBody("Reclamado por: ${review.claimantName ?: review.claim.claimantId}")
            val answered = review.answers.count { it.answer != null }
            AppLabel(
                if (answered == 0) {
                    "Aún no respondió las preguntas de control"
                } else {
                    "Sugerencia: ${review.matchingAnswers} de ${review.answers.size} respuestas coinciden"
                }
            )
        }
    }
}

internal fun ClaimStatus.tone(): StatusTone = when (this) {
    ClaimStatus.PENDIENTE -> StatusTone.Warning
    ClaimStatus.EN_VERIFICACION -> StatusTone.Info
    ClaimStatus.APROBADO, ClaimStatus.ENTREGADO -> StatusTone.Success
    ClaimStatus.RECHAZADO -> StatusTone.Error
}
