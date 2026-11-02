package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/**
 * Pantalla de éxito del envío (no existe en el PDF/Figma): se arma solo con componentes y tokens del DS.
 * "Te avisaremos si hay coincidencias" + acceso a Mis reportes (L05) o volver al inicio.
 */
@Composable
internal fun ReportSuccessContent(report: LostReport, onGoToMyReports: () -> Unit, onFinish: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    val spacing = UcbEncuentraTheme.spacing
    // Mismo header azul que el wizard: la barra de estado usa íconos claros en toda la app.
    Column(Modifier.fillMaxSize()) {
        CurvedHeader(title = "Reportar objeto", centerTitle = true)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier.size(96.dp).background(colors.success, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(DsIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(56.dp))
            }
            AppHeading("¡Reporte enviado!", textAlign = TextAlign.Center)
            AppBody(
                "Tu reporte ya está activo. Lo compararemos con los objetos encontrados en el campus y " +
                    "te avisaremos si hay coincidencias.",
                textAlign = TextAlign.Center
            )

            AppCard(modifier = Modifier.fillMaxWidth()) {
                AppLabel("Reportaste")
                AppTitle(report.title)
                AppBody("${report.category.label} / ${report.subcategory.label} · ${report.zone.label}")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.secondary.copy(alpha = 0.25f), UcbEncuentraTheme.shapes.medium)
                    .padding(spacing.itemGap),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = colors.tertiary)
                AppLabel(
                    "Recibirás una notificación en Alertas. También puedes seguir el estado en Mis reportes.",
                    textAlign = TextAlign.Center
                )
            }

            PrimaryButton(
                text = "Ver mis reportes",
                onClick = onGoToMyReports,
                size = AppButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(text = "Volver al inicio", onClick = onFinish, modifier = Modifier.fillMaxWidth())
        }
    }
}
