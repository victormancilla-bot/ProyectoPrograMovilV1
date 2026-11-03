package bo.edu.ucb.encuentra.feature.dashboard.presentation.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.formatRelative
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.StatsSource
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardEffect
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardEvent
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardState
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardViewModel
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.StatTarget
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import androidx.compose.material.icons.outlined.History
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.StatCard
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
import kotlin.time.Instant

/** Figma "Dashboard Admin" (D01): contadores en vivo, acción principal y últimos reclamos pendientes. */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onRegisterFound: () -> Unit,
    onOpenClaims: () -> Unit,
    onOpenClaim: (claimId: String) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenRetention: () -> Unit = {},
    onOpenAudit: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToFoundForm -> onRegisterFound()
                DashboardEffect.NavigateToClaims -> onOpenClaims()
                DashboardEffect.NavigateToInventory -> onOpenInventory()
                DashboardEffect.NavigateToRetention -> onOpenRetention()
                DashboardEffect.NavigateToAudit -> onOpenAudit()
                is DashboardEffect.NavigateToClaimDetail -> onOpenClaim(effect.claimId)
            }
        }
    }

    DashboardContent(state, viewModel::handleEvent)
}

@Composable
private fun DashboardContent(state: DashboardState, onEvent: (DashboardEvent) -> Unit) {
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = if (state.campus.isEmpty()) "Panel" else "Panel — ${state.campus}",
                subtitle = "Administración",
                centerTitle = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val stats = state.stats
                when {
                    stats != null -> DashboardBody(stats, state, onEvent)
                    state.error != null -> ErrorState(message = state.error, onRetry = { onEvent(DashboardEvent.OnRetry) })
                    else -> LoadingState(message = "Cargando panel...")
                }
            }
        }
    }
}

@Composable
private fun DashboardBody(stats: DashboardStats, state: DashboardState, onEvent: (DashboardEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        state.source?.let { LiveIndicator(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            StatCard(
                value = stats.inCustody.toString(), label = "Objetos en custodia", icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f), onClick = { onEvent(DashboardEvent.OnStatClicked(StatTarget.INVENTORY)) }
            )
            StatCard(
                value = stats.pendingDelivery.toString(), label = "Pendientes de entrega", icon = Icons.Outlined.AssignmentTurnedIn,
                modifier = Modifier.weight(1f), onClick = { onEvent(DashboardEvent.OnStatClicked(StatTarget.CLAIMS)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            StatCard(
                value = stats.inVerification.toString(), label = "En verificación", icon = Icons.Outlined.FactCheck,
                modifier = Modifier.weight(1f), onClick = { onEvent(DashboardEvent.OnStatClicked(StatTarget.CLAIMS)) }
            )
            // Mockup: "Por vencer" en rojo para llamar la atención.
            StatCard(
                value = stats.expiringSoon.toString(), label = "Por vencer", icon = Icons.Outlined.EventBusy,
                valueColor = colors.error, modifier = Modifier.weight(1f),
                // D04: abre la pantalla de Retención (vencidos y por vencer).
                onClick = { onEvent(DashboardEvent.OnStatClicked(StatTarget.RETENTION)) }
            )
        }

        AppOverline("Acciones")
        PrimaryButton(
            text = "Registrar objeto encontrado",
            onClick = { onEvent(DashboardEvent.OnRegisterFound) },
            size = AppButtonSize.Large,
            leadingIcon = DsIcons.Add,
            modifier = Modifier.fillMaxWidth()
        )
        // S05: quién hizo qué y cuándo (reclamos, entregas, donaciones, roles).
        SecondaryButton(
            text = "Registro de auditoría",
            onClick = { onEvent(DashboardEvent.OnOpenAudit) },
            leadingIcon = Icons.Outlined.History,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppOverline("Últimos reclamos pendientes", modifier = Modifier.weight(1f))
            TextButton(onClick = { onEvent(DashboardEvent.OnSeeAllClaims) }) {
                AppLabel("Ver todos", color = colors.tertiary)
            }
        }
        if (state.latestClaims.isEmpty()) {
            AppBody("No hay reclamos pendientes.")
        } else {
            state.latestClaims.forEach { claim ->
                ClaimRow(claim, state.now, onClick = { onEvent(DashboardEvent.OnClaimClicked(claim.claimId.value)) })
            }
        }
        Spacer(Modifier.height(spacing.medium))
    }
}

/** Punto verde + de dónde vienen los números: deja claro en la demo que se actualizan solos. */
@Composable
private fun LiveIndicator(source: StatsSource) {
    val colors = UcbEncuentraTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(colors.success, CircleShape))
        AppLabel(
            when (source) {
                StatsSource.REALTIME_DATABASE -> "En vivo · Firebase Realtime Database"
                StatsSource.LOCAL -> "En vivo · datos del dispositivo"
            }
        )
    }
}

@Composable
private fun ClaimRow(claim: PendingClaim, now: Instant?, onClick: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(UcbEncuentraTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
        ) {
            Box(
                Modifier.size(48.dp).background(colors.surfaceVariant, UcbEncuentraTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(claim.category.icon(), contentDescription = null, tint = colors.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                AppTitle(claim.itemTitle)
                val time = now?.let { " • ${formatRelative(claim.createdAt, it)}" }.orEmpty()
                AppLabel("Reclamo #${claim.number} · Reclamado por: ${claim.claimantName}$time")
            }
        }
    }
}
