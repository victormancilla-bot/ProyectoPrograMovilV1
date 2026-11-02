package bo.edu.ucb.encuentra.feature.designsystem.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.list.AppListGroup
import edu.ucb.ds.components.list.AppListItem
import edu.ucb.ds.components.progress.StepProgressBar
import edu.ucb.ds.components.selection.CheckRow
import edu.ucb.ds.components.selection.ColorSwatchPicker
import edu.ucb.ds.components.textfields.AppTextArea
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.components.textfields.SelectField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Componentes atómicos (F03) funcionando, para revisarlos contra el Figma. */
@Composable
internal fun ComponentsSection() {
    val spacing = UcbEncuentraTheme.spacing
    var email by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var features by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("Todos") }
    var color by rememberSaveable { mutableStateOf<String?>("Azul") }
    var check1 by rememberSaveable { mutableStateOf(false) }
    var check2 by rememberSaveable { mutableStateOf(true) }
    var step by rememberSaveable { mutableStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        AppOverline("Componentes (F03)")

        AppOverline("Botones")
        PrimaryButton("Continuar", onClick = {}, modifier = Modifier.fillMaxWidth(), trailingIcon = DsIcons.ArrowForward)
        PrimaryButton(
            "Siguiente",
            onClick = { step = step % 4 + 1 },
            modifier = Modifier.fillMaxWidth(),
            size = AppButtonSize.Large
        )
        PrimaryButton("Siguiente", onClick = {}, modifier = Modifier.fillMaxWidth(), size = AppButtonSize.Large, enabled = false)
        SecondaryButton("NO ES LO MÍO, SEGUIR BUSCANDO", onClick = {}, modifier = Modifier.fillMaxWidth())
        SecondaryButton(
            "Agregar foto de referencia (opcional)",
            onClick = {},
            color = UcbEncuentraTheme.colors.tertiary,
            leadingIcon = DsIcons.Add
        )

        AppOverline("Barra de pasos")
        Box(
            Modifier
                .fillMaxWidth()
                .clip(UcbEncuentraTheme.shapes.large)
                .background(UcbEncuentraTheme.colors.primary)
                .padding(spacing.screenPadding)
        ) {
            StepProgressBar(currentStep = step, totalSteps = 4, stepName = "Categoría", onDark = true)
        }
        StepProgressBar(currentStep = step, totalSteps = 4)

        AppOverline("Campos")
        AppCard {
            AppTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "correo@ucb.edu.bo",
                containerColor = UcbEncuentraTheme.colors.surface,
                errorMessage = if (email.isNotEmpty() && !email.endsWith("@ucb.edu.bo")) "Usa tu correo @ucb.edu.bo" else null
            )
            SelectField(
                options = listOf("Biblioteca Central", "Bloque D", "Cafetería", "Canchas deportivas"),
                selected = place,
                onSelect = { place = it },
                label = "¿Dónde lo perdiste?",
                placeholder = "Selecciona una ubicación"
            )
            AppTextField(
                value = detail,
                onValueChange = { detail = it },
                label = "Detalle adicional (aula, piso, etc.)",
                placeholder = "Ej. Aula A-203, cerca de la ventana"
            )
            ColorSwatchPicker(selected = color, onSelect = { color = it.name }, label = "Color principal", onAddClick = {})
            AppTextArea(
                value = features,
                onValueChange = { features = it },
                label = "Características distintivas",
                placeholder = "Ej. sticker roto en la esquina, rasguño en la parte trasera..."
            )
        }

        AppOverline("Chips")
        AppChipGroup(options = listOf("Todos", "Buscando", "Verificación"), selected = filter, onSelect = { filter = it })

        AppOverline("Tarjetas y listas")
        AppCard(variant = AppCardVariant.Filled) {
            AppHeading("¿Perdiste algo?")
            AppBody("La comunidad UCB está aquí para ayudarte a recuperarlo.")
            PrimaryButton("Reportar objeto", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = DsIcons.Add)
        }
        AppListGroup {
            AppListItem("Mis reportes", leadingIcon = DsIcons.Check, onClick = {})
            AppListItem("Objetos encontrados", leadingIcon = DsIcons.ChevronDown, onClick = {})
            AppListItem("Cómo funciona", leadingIcon = DsIcons.Add, onClick = {})
        }

        AppOverline("Verificación física")
        AppCard {
            CheckRow(
                "Confirmo que he verificado la identidad del estudiante usando su Carnet Universitario.",
                checked = check1,
                onCheckedChange = { check1 = it }
            )
            CheckRow("El estudiante ha recibido el objeto.", checked = check2, onCheckedChange = { check2 = it })
        }
    }
}
