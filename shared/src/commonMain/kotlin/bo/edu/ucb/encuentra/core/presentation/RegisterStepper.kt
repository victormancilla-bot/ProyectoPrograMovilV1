package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Pasos del registro de un objeto encontrado (mockup "Registrar – Captura de Foto"). */
val REGISTER_STEPS = listOf("Captura", "Detalles", "Ubicación")

/**
 * "1. Captura —— 2. Detalles —— 3. Ubicación". [activeSteps] marca los pasos de la pantalla actual: la captura es el
 * paso 1 y el formulario (H03) contiene Detalles y Ubicación juntos.
 */
@Composable
fun RegisterStepper(activeSteps: Set<Int>, modifier: Modifier = Modifier) {
    val colors = UcbEncuentraTheme.colors
    val lastActive = activeSteps.maxOrNull() ?: 1
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        REGISTER_STEPS.forEachIndexed { index, name ->
            val step = index + 1
            val reached = step <= lastActive
            AppLabel("$step. $name", color = if (step in activeSteps) colors.primary else if (reached) colors.onSurfaceVariant else colors.outline)
            if (step < REGISTER_STEPS.size) {
                Box(
                    Modifier.weight(1f).height(1.dp)
                        .background(if (step < lastActive) colors.primary else colors.outlineVariant)
                )
            }
        }
    }
}
