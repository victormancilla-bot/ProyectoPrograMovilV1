package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.theme.UcbEncuentraTheme

/** `maintenance_mode` activo en Remote Config (F08): la app se pausa sin publicar una versión nueva. */
@Composable
fun MaintenanceScreen(supportContact: String, retrying: Boolean, onRetry: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Build, contentDescription = null, tint = colors.primary, modifier = Modifier.size(72.dp))
            AppHeading("Estamos en mantenimiento", textAlign = TextAlign.Center)
            AppBody(
                "UCB Encuentra vuelve en un momento. Tus reportes y reclamos están guardados.",
                textAlign = TextAlign.Center
            )
            AppLabel("¿Es urgente? Escribe a $supportContact", textAlign = TextAlign.Center)
            PrimaryButton(
                text = if (retrying) "Verificando..." else "Reintentar",
                onClick = onRetry,
                enabled = !retrying,
                size = AppButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
