package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.presentation.AllItemColors
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportDraftState
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.selection.ColorSwatchPicker
import edu.ucb.ds.components.selection.DefaultItemColors
import edu.ucb.ds.components.textfields.AppTextArea
import edu.ucb.ds.components.textfields.AppTextField

/** Paso 2 (Figma "Reportar - Atributos"): nombre del objeto, color, marca opcional y características. */
@Composable
internal fun AttributesStep(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val draft = state.draft
    AppCard {
        AppHeading("¿Qué detalles tiene?")

        AppTextField(
            value = draft.title,
            onValueChange = { onEvent(ReportWizardEvent.OnTitleChanged(it)) },
            label = "¿Qué es?",
            placeholder = "Ej. Laptop Dell XPS",
            // "Muy corto" solo si ya empezó a escribir: el botón deshabilitado basta para el campo vacío.
            errorMessage = state.visibleError(
                ReportFieldError.TITLE_TOO_LONG,
                ReportFieldError.TITLE_TOO_SHORT.takeIf { draft.title.isNotBlank() }
            )
        )

        ColorSwatchPicker(
            selected = draft.color,
            onSelect = { onEvent(ReportWizardEvent.OnColorSelected(it.name)) },
            options = if (state.showAllColors) AllItemColors else DefaultItemColors,
            label = "Color principal",
            onAddClick = if (state.showAllColors) null else ({ onEvent(ReportWizardEvent.OnShowAllColors) })
        )

        AppTextField(
            value = draft.brand,
            onValueChange = { onEvent(ReportWizardEvent.OnBrandChanged(it)) },
            label = "Marca (opcional)",
            placeholder = "Ej. Apple, Stanley...",
            errorMessage = state.visibleError(ReportFieldError.BRAND_TOO_LONG)
        )

        AppTextArea(
            value = draft.features,
            onValueChange = { onEvent(ReportWizardEvent.OnFeaturesChanged(it)) },
            label = "Características distintivas",
            placeholder = "Ej. sticker roto en la esquina, rasguño en la parte trasera...",
            errorMessage = state.visibleError(ReportFieldError.FEATURES_TOO_LONG)
        )
    }
}

/** El primer error de [candidates] que el paso tiene ahora, o null. */
private fun ReportDraftState.visibleError(vararg candidates: ReportFieldError?): String? =
    candidates.firstOrNull { it != null && it in errors }?.message
