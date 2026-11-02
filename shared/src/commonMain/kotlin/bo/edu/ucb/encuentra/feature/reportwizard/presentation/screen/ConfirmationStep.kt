package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.formatDateRange
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportDraftState
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import coil3.compose.AsyncImage
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Paso 4 (Figma "Reportar - Confirmación"): resumen con "Editar" por sección, que vuelve a ese paso. */
@Composable
internal fun ConfirmationStep(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val draft = state.draft
    val spacing = UcbEncuentraTheme.spacing
    val edit = { step: Int -> onEvent(ReportWizardEvent.OnEditStep(step)) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            AppHeading("Revisa tu reporte")
            AppBody("Confirma que los detalles de tu objeto perdido sean correctos antes de enviarlos a la red del campus.")
        }

        AppCard(variant = AppCardVariant.Filled) {
            Section("Detalles del objeto", onEdit = { edit(ValidateReportStepUseCase.STEP_CATEGORY) }) {
                SummaryRow(
                    icon = draft.category?.icon() ?: Icons.Filled.Description,
                    label = "Categoría",
                    value = listOfNotNull(draft.category?.label, draft.subcategory?.label).joinToString(" / ")
                )
            }
            Section("Atributos", onEdit = { edit(ValidateReportStepUseCase.STEP_ATTRIBUTES) }) {
                SummaryRow(
                    icon = Icons.Filled.Palette,
                    label = "Objeto",
                    value = listOfNotNull(
                        draft.title.trim(),
                        draft.color,
                        draft.brand.trim().ifEmpty { null }
                    ).joinToString(" · ")
                )
                if (draft.features.isNotBlank()) {
                    SummaryRow(icon = Icons.Filled.Description, label = "Descripción", value = draft.features.trim())
                }
            }
            Section("Cuándo y dónde", onEdit = { edit(ValidateReportStepUseCase.STEP_WHERE_WHEN) }) {
                val from = draft.lostFrom
                val to = draft.lostTo
                if (from != null && to != null) {
                    SummaryRow(
                        icon = DsIcons.Calendar,
                        label = if (from == to) "Fecha de pérdida" else "Entre",
                        value = listOfNotNull(formatDateRange(from, to), draft.timeSlot?.label).joinToString(" · ")
                    )
                }
                SummaryRow(
                    icon = DsIcons.Location,
                    label = "Última ubicación conocida",
                    value = listOfNotNull(draft.zone?.label, draft.locationDetail.trim().ifEmpty { null }).joinToString(", ")
                )
                draft.photoUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Foto de referencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(UcbEncuentraTheme.shapes.small)
                    )
                }
            }
            Section("Contacto", onEdit = { edit(ValidateReportStepUseCase.STEP_WHERE_WHEN) }, divider = false) {
                SummaryRow(icon = Icons.Filled.Email, label = null, value = draft.contactEmail)
                SummaryRow(
                    icon = Icons.Filled.Phone,
                    label = null,
                    value = draft.contactPhone.trim().ifEmpty { "Sin teléfono" }
                )
            }
        }

        InfoNote("Al enviar, tu reporte será comparado automáticamente con los objetos encontrados registrados. Recibirás una notificación si hay una coincidencia.")

        state.submitError?.let { AppLabel(it, color = UcbEncuentraTheme.colors.error) }
    }
}

@Composable
private fun Section(
    title: String,
    onEdit: () -> Unit,
    divider: Boolean = true,
    content: @Composable () -> Unit
) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppOverline(title, color = UcbEncuentraTheme.colors.onSurface, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clip(UcbEncuentraTheme.shapes.small).clickable(onClick = onEdit).padding(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = UcbEncuentraTheme.colors.tertiary, modifier = Modifier.size(14.dp))
                AppLabel("Editar", color = UcbEncuentraTheme.colors.tertiary)
            }
        }
        content()
        if (divider) HorizontalDivider(color = UcbEncuentraTheme.colors.outlineVariant)
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String?, value: String) {
    val colors = UcbEncuentraTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column {
            if (label != null) AppLabel(label)
            AppBody(value, color = colors.onSurface)
        }
    }
}

/** Nota azul del mockup bajo el botón de enviar. */
@Composable
private fun InfoNote(text: String) {
    val colors = UcbEncuentraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.secondary.copy(alpha = 0.25f), UcbEncuentraTheme.shapes.medium)
            .padding(UcbEncuentraTheme.spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
        AppLabel(text, color = colors.onSurfaceVariant)
    }
}
