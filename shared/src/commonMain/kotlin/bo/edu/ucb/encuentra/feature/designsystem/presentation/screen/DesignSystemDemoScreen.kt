package bo.edu.ucb.encuentra.feature.designsystem.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import edu.ucb.ds.theme.UcbEncuentraTheme
import edu.ucb.ds.theme.scoreColor

/**
 * Pantalla de prueba: muestra SOLO tokens de la librería edu.ucb.ds:shared (Design System).
 * Si compila y se ve, la librería se está consumiendo bien desde ds-repo.
 * El switch prueba el modo oscuro.
 */
@Composable
fun DesignSystemDemoScreen(onNavigateBack: () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(systemDark) }

    UcbEncuentraTheme(darkTheme = darkTheme) {
        val colors = UcbEncuentraTheme.colors
        val spacing = UcbEncuentraTheme.spacing

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            TextButton(onClick = onNavigateBack) { Text("← Volver") }
            Text("Design System UCB Encuentra", style = UcbEncuentraTheme.typography.heading, color = colors.onBackground)
            Text("Valores del Figma", style = UcbEncuentraTheme.typography.body, color = colors.onSurfaceVariant)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                Text("Modo oscuro", style = UcbEncuentraTheme.typography.body, color = colors.onBackground)
            }

            ColorsSection()
            ScoreSection()
            TypographySection()
            SpacingSection()
            ShapesSection()
            ComponentsSection()
            DomainComponentsSection()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), style = UcbEncuentraTheme.typography.label, color = UcbEncuentraTheme.colors.onBackground)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsSection() {
    val c = UcbEncuentraTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
        SectionTitle("Colors")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)
        ) {
            listOf(
                "primary" to c.primary, "secondary" to c.secondary, "onSecondary" to c.onSecondary,
                "tertiary" to c.tertiary, "background" to c.background, "surface" to c.surface,
                "surfaceContainer" to c.surfaceContainer, "surfaceVariant" to c.surfaceVariant,
                "onSurface" to c.onSurface, "onSurfaceVariant" to c.onSurfaceVariant,
                "outline" to c.outline, "outlineVariant" to c.outlineVariant, "error" to c.error,
                "success" to c.success, "warning" to c.warning, "neutral" to c.neutral
            ).forEach { (name, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                    Box(
                        Modifier.size(48.dp)
                            .background(color, UcbEncuentraTheme.shapes.medium)
                            .border(1.dp, c.outlineVariant, UcbEncuentraTheme.shapes.medium)
                    )
                    Text(name, style = UcbEncuentraTheme.typography.label, color = c.onBackground)
                }
            }
        }
    }
}

@Composable
private fun ScoreSection() {
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
        SectionTitle("Score")
        Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
            listOf(92, 65, 30).forEach { score ->
                Box(
                    modifier = Modifier.size(48.dp).background(UcbEncuentraTheme.colors.scoreColor(score), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$score%", style = UcbEncuentraTheme.typography.label, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TypographySection() {
    val t = UcbEncuentraTheme.typography
    val color = UcbEncuentraTheme.colors.onBackground
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
        SectionTitle("Typography · Open Sans")
        Text("Heading · ¿Perdiste algo?", style = t.heading, color = color)
        Text("Title · 2 reportes activos", style = t.title, color = color)
        Text("Body large · Mis reportes", style = t.bodyLarge, color = color)
        Text("Body · Ingresa con tu correo institucional @ucb.edu.bo", style = t.body, color = color)
        Text("Label · Continuar", style = t.label, color = color)
        Text("OVERLINE · ACTIVIDAD", style = t.overline, color = UcbEncuentraTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun SpacingSection() {
    val s = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
        SectionTitle("Spacing")
        listOf(
            "small 4" to s.small, "medium 8" to s.medium, "itemGap 12" to s.itemGap,
            "large 16" to s.large, "screen 20" to s.screenPadding
        ).forEach { (name, size) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = UcbEncuentraTheme.typography.label,
                    color = UcbEncuentraTheme.colors.onBackground,
                    modifier = Modifier.width(72.dp)
                )
                Box(Modifier.width(size).height(12.dp).background(UcbEncuentraTheme.colors.primary))
            }
        }
    }
}

@Composable
private fun ShapesSection() {
    val sh = UcbEncuentraTheme.shapes
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
        SectionTitle("Shapes")
        Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
            listOf(
                "small" to sh.small, "medium" to sh.medium, "large" to sh.large,
                "xLarge" to sh.extraLarge, "pill" to sh.pill
            ).forEach { (name, shape) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(56.dp).background(UcbEncuentraTheme.colors.surfaceContainer, shape))
                    Text(name, style = UcbEncuentraTheme.typography.label, color = UcbEncuentraTheme.colors.onBackground)
                }
            }
        }
    }
}
