package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.presentation.ClickableField
import bo.edu.ucb.encuentra.core.presentation.PastDatePickerDialog
import bo.edu.ucb.encuentra.core.presentation.formatDmy
import bo.edu.ucb.encuentra.core.presentation.rememberPhotoPicker
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.TimeSlot
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportDraftState
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.components.textfields.SelectField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/**
 * Paso 3 (Figma "Reportar - Ubicación y Foto") con las correcciones de L03: rango de fechas dd/mm/aaaa
 * en vez de una sola fecha mm/dd/yyyy, franja horaria y contacto (la confirmación lo muestra pero el mockup no lo pide).
 */
@Composable
internal fun WhereWhenStep(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        AppHeading("¿Dónde y cuándo?")
        WhereWhenCard(state, onEvent)
        ReferencePhoto(state.draft.photoUri, onEvent)
        ContactCard(state, onEvent)
    }
}

@Composable
private fun WhereWhenCard(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val draft = state.draft
    var picker by remember { mutableStateOf<String?>(null) }

    AppCard {
        SelectField(
            options = CampusZone.entries.map { it.label },
            selected = draft.zone?.label,
            onSelect = { label ->
                CampusZone.entries.firstOrNull { it.label == label }?.let { onEvent(ReportWizardEvent.OnZoneSelected(it)) }
            },
            label = "¿Dónde lo perdiste?",
            placeholder = "Selecciona una ubicación"
        )
        AppTextField(
            value = draft.locationDetail,
            onValueChange = { onEvent(ReportWizardEvent.OnLocationDetailChanged(it)) },
            label = "Detalle adicional (aula, piso, etc.)",
            placeholder = "Ej. Aula A-203, cerca de la ventana",
            errorMessage = state.errorIfAny(ReportFieldError.DETAIL_TOO_LONG)
        )

        AppOverline("¿Cuándo?", color = UcbEncuentraTheme.colors.onSurface)
        AppBody("Si no estás seguro del día, marca un rango.")
        Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
            ClickableField(
                value = draft.lostFrom?.formatDmy().orEmpty(),
                label = "Desde",
                icon = DsIcons.Calendar,
                onClick = { picker = "from" },
                modifier = Modifier.weight(1f)
            )
            ClickableField(
                value = draft.lostTo?.formatDmy().orEmpty(),
                label = "Hasta",
                icon = DsIcons.Calendar,
                onClick = { picker = "to" },
                modifier = Modifier.weight(1f)
            )
        }
        state.errorIfAny(
            ReportFieldError.DATE_RANGE_INVERTED,
            ReportFieldError.DATE_IN_FUTURE,
            ReportFieldError.DATE_RANGE_TOO_WIDE
        )?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }

        AppLabel("Franja horaria")
        AppChipGroup(
            options = TimeSlot.entries.map { it.label },
            selected = draft.timeSlot?.label.orEmpty(),
            onSelect = { label ->
                TimeSlot.entries.firstOrNull { it.label == label }?.let { onEvent(ReportWizardEvent.OnTimeSlotSelected(it)) }
            }
        )
    }

    when (picker) {
        "from" -> PastDatePickerDialog(
            initial = draft.lostFrom,
            onPick = { onEvent(ReportWizardEvent.OnLostFromSelected(it)) },
            onDismiss = { picker = null }
        )
        "to" -> PastDatePickerDialog(
            initial = draft.lostTo,
            onPick = { onEvent(ReportWizardEvent.OnLostToSelected(it)) },
            onDismiss = { picker = null }
        )
    }
}

/** Caja punteada del mockup: foto similar al objeto, opcional. Solo galería por ahora; la cámara llega con H01. */
@Composable
private fun ReferencePhoto(photoUri: String?, onEvent: (ReportWizardEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    val shape = UcbEncuentraTheme.shapes.extraLarge
    val picker = rememberPhotoPicker { onEvent(ReportWizardEvent.OnPhotoPicked(it)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.outline,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                )
            }
            .padding(UcbEncuentraTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto de referencia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(shape)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
                if (picker.isAvailable) {
                    SecondaryButton(text = "Cambiar", onClick = picker::launch, modifier = Modifier.weight(1f))
                }
                SecondaryButton(
                    text = "Quitar",
                    onClick = { onEvent(ReportWizardEvent.OnPhotoRemoved) },
                    color = colors.error,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (picker.isAvailable) {
            SecondaryButton(
                text = "Agregar foto de referencia (opcional)",
                onClick = picker::launch,
                leadingIcon = Icons.Filled.AddAPhoto,
                modifier = Modifier.fillMaxWidth()
            )
            AppBody(
                "Sube una foto similar al objeto que perdiste para ayudar a identificarlo.",
                textAlign = TextAlign.Center
            )
        } else {
            AppBody("La foto de referencia todavía no está disponible en este dispositivo.", textAlign = TextAlign.Center)
        }
    }
}

/** Contacto: el correo sale del perfil (no se edita); el teléfono es opcional. */
@Composable
private fun ContactCard(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val draft = state.draft
    AppCard {
        AppOverline("Contacto", color = UcbEncuentraTheme.colors.onSurface)
        AppBody("Te avisaremos por aquí si aparece una coincidencia.")
        AppTextField(
            value = draft.contactEmail,
            onValueChange = {},
            label = "Correo (de tu perfil)",
            leadingIcon = Icons.Filled.Email,
            enabled = false
        )
        AppTextField(
            value = draft.contactPhone,
            onValueChange = { onEvent(ReportWizardEvent.OnPhoneChanged(it)) },
            label = "Teléfono / WhatsApp (opcional)",
            placeholder = "Ej. 71234567",
            leadingIcon = Icons.Filled.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            errorMessage = state.errorIfAny(ReportFieldError.PHONE_INVALID)
        )
    }
}

/**
 * Errores que se muestran apenas aparecen (los provoca lo que el usuario eligió o escribió).
 * Lo que falta elegir (zona, franja) no se marca: basta con Siguiente deshabilitado.
 */
private fun ReportDraftState.errorIfAny(vararg candidates: ReportFieldError): String? =
    candidates.firstOrNull { it in errors }?.message
