package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.resources.Res
import bo.edu.ucb.encuentra.resources.ucb_escudo
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.jetbrains.compose.resources.painterResource

/**
 * F11 · Splash mientras arranca la app (datos de prueba, sesión guardada, configuración remota). Mismo azul UCB
 * que el splash del sistema de Android 12+ (`Theme.UcbEncuentra`), así no hay un salto de color.
 */
@Composable
fun SplashScreen() {
    val colors = UcbEncuentraTheme.colors
    Box(
        Modifier.fillMaxSize().background(colors.primary).semantics { contentDescription = "Cargando UCB Encuentra" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
            Image(painter = painterResource(Res.drawable.ucb_escudo), contentDescription = null, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(UcbEncuentraTheme.spacing.medium))
            AppHeading("UCB Encuentra", color = colors.onPrimary)
            AppBody("Objetos perdidos y encontrados", color = colors.onPrimary)
            Spacer(Modifier.height(UcbEncuentraTheme.spacing.large))
            CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}
