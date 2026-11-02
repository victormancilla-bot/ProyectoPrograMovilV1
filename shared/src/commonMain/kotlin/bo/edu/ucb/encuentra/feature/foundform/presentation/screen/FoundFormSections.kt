package bo.edu.ucb.encuentra.feature.foundform.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.presentation.AllItemColors
import bo.edu.ucb.encuentra.core.presentation.ClickableField
import bo.edu.ucb.encuentra.core.presentation.PastDatePickerDialog
import bo.edu.ucb.encuentra.core.presentation.TimePickerDialog
import bo.edu.ucb.encuentra.core.presentation.formatDmy
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.icon
import bo.edu.ucb.encuentra.core.presentation.swatchFor
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.FoundFieldError
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormEvent
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormState
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.selection.ColorSwatchPicker
import edu.ucb.ds.components.selection.DefaultItemColors
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

private enum class DetectedEditor { CATEGORY, COLOR }

/**
 * Tarjeta "Datos detectados" del mockup: categoría y color, editables tocando cada fila.
 * En R3 se eligen a mano ("Ingreso manual"); con H02 la IA los precarga y el chip dice "Sugerido por IA".
 */
@Composable
internal fun DetectedDataCard(state: FoundFormState, onEvent: (FoundFormEvent) -> Unit) {
    val draft = state.draft
    val spacing = UcbEncuentraTheme.spacing
    var editing by rememberSaveable { mutableStateOf<DetectedEditor?>(null) }

    AppCard(variant = AppCardVariant.Filled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppOverline("Datos detectados", color = UcbEncuentraTheme.colors.onSurface)
            if (draft.suggestedByAi) {
                StatusChip("Sugerido por IA", StatusTone.Warning)
            } else {
                StatusChip("Ingreso manual", StatusTone.Neutral)
            }
        }

        val category = draft.category
        DetectedRow(
            label = "Categoría",
            value = when {
                category == null -> "Elegir categoría"
                draft.subcategory == null -> category.label
                else -> "${category.label} / ${draft.subcategory.label}"
            },
            icon = category?.icon() ?: Icons.Filled.Category,
            expanded = editing == DetectedEditor.CATEGORY,
            onClick = { editing = if (editing == DetectedEditor.CATEGORY) null else DetectedEditor.CATEGORY }
        )
        if (editing == DetectedEditor.CATEGORY) {
            AppChipGroup(
                options = Category.entries.map { it.label },
                selected = category?.label.orEmpty(),
                onSelect = { label ->
                    Category.entries.firstOrNull { it.label == label }?.let { onEvent(FoundFormEvent.OnCategorySelected(it)) }
                }
            )
            if (category != null) {
                AppOverline("¿Qué tipo de ${category.label.lowercase()}?")
                AppChipGroup(
                    options = category.subcategories.map { it.label },
                    selected = draft.subcategory?.label.orEmpty(),
                    onSelect = { label ->
                        category.subcategories.firstOrNull { it.label == label }?.let {
                            onEvent(FoundFormEvent.OnSubcategorySelected(it))
                            editing = null
                        }
                    }
                )
            }
        }
        FieldError(state.errorFor(FoundFieldError.CATEGORY_REQUIRED, FoundFieldError.SUBCATEGORY_REQUIRED))

        val swatch = swatchFor(draft.color)
        DetectedRow(
            label = "Color predominante",
            value = draft.color ?: "Elegir color",
            icon = Icons.Filled.Palette,
            iconBackground = swatch?.color,
            expanded = editing == DetectedEditor.COLOR,
            onClick = { editing = if (editing == DetectedEditor.COLOR) null else DetectedEditor.COLOR }
        )
        if (editing == DetectedEditor.COLOR) {
            ColorSwatchPicker(
                selected = draft.color,
                onSelect = {
                    onEvent(FoundFormEvent.OnColorSelected(it.name))
                    editing = null
                },
                options = if (state.showAllColors) AllItemColors else DefaultItemColors,
                onAddClick = if (state.showAllColors) null else ({ onEvent(FoundFormEvent.OnShowAllColors) })
            )
        }
        FieldError(state.errorFor(FoundFieldError.COLOR_REQUIRED))
    }
}

/** Fila blanca de la tarjeta: ícono en círculo, etiqueta y valor (Figma: "Categoría / Electrónica"). */
@Composable
private fun DetectedRow(
    label: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    iconBackground: androidx.compose.ui.graphics.Color? = null
) {
    val colors = UcbEncuentraTheme.colors
    val spacing = UcbEncuentraTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background, UcbEncuentraTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground ?: colors.secondary, CircleShape)
                .border(1.dp, colors.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Con un color elegido, el círculo ES el color (sin ícono encima).
            if (iconBackground == null) Icon(icon, contentDescription = null, tint = colors.primary)
        }
        Column(Modifier.weight(1f)) {
            AppLabel(label)
            AppTitle(value)
        }
        Icon(
            if (expanded) DsIcons.ChevronDown else DsIcons.ChevronRight,
            contentDescription = if (expanded) "Cerrar" else "Editar $label",
            tint = colors.onSurfaceVariant
        )
    }
}

/** Fecha y hora del hallazgo: campos que abren el selector (no se escriben a mano). */
@Composable
internal fun FoundDateTimeFields(state: FoundFormState, onEvent: (FoundFormEvent) -> Unit) {
    val draft = state.draft
    var picker by remember { mutableStateOf<String?>(null) }
    val error = state.errorFor(FoundFieldError.FOUND_DATE_REQUIRED, FoundFieldError.FOUND_DATE_IN_FUTURE)

    Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
        ClickableField(
            value = draft.foundDate?.formatDmy().orEmpty(),
            label = "Fecha de hallazgo",
            icon = DsIcons.Calendar,
            errorMessage = error,
            onClick = { picker = "date" },
            modifier = Modifier.weight(1.3f)
        )
        ClickableField(
            value = draft.foundTime?.formatHm().orEmpty(),
            label = "Hora",
            errorMessage = if (error != null) " " else null,
            onClick = { picker = "time" },
            modifier = Modifier.weight(1f)
        )
    }

    when (picker) {
        "date" -> PastDatePickerDialog(
            initial = draft.foundDate,
            onPick = { onEvent(FoundFormEvent.OnFoundDateSelected(it)) },
            onDismiss = { picker = null }
        )
        "time" -> TimePickerDialog(
            initial = draft.foundTime,
            onPick = { onEvent(FoundFormEvent.OnFoundTimeSelected(it)) },
            onDismiss = { picker = null }
        )
    }
}

/** 1-3 preguntas que solo el dueño sabría responder; las usa la verificación del reclamo (V01). */
@Composable
internal fun ControlQuestions(state: FoundFormState, onEvent: (FoundFormEvent) -> Unit) {
    val questions = state.draft.controlQuestions
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
        AppOverline("Preguntas de control", color = UcbEncuentraTheme.colors.onSurface)
        AppBody(
            "Algo que solo el dueño sabría (no lo pongas en la descripción). Se le preguntará al reclamar y la app " +
                "comparará su respuesta con la que escribas aquí. La respuesta esperada solo la ve el staff."
        )
        questions.forEachIndexed { index, question ->
            Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.small)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        value = question,
                        onValueChange = { onEvent(FoundFormEvent.OnQuestionChanged(index, it)) },
                        placeholder = QUESTION_EXAMPLES.getOrElse(index) { QUESTION_EXAMPLES.first() },
                        modifier = Modifier.weight(1f)
                    )
                    if (questions.size > 1) {
                        IconButton(onClick = { onEvent(FoundFormEvent.OnRemoveQuestion(index)) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar pregunta ${index + 1}")
                        }
                    }
                }
                AppTextField(
                    value = state.draft.expectedAnswers.getOrElse(index) { "" },
                    onValueChange = { onEvent(FoundFormEvent.OnExpectedAnswerChanged(index, it)) },
                    placeholder = "Respuesta esperada (privada)",
                    leadingIcon = Icons.Outlined.Lock,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        FieldError(
            state.errorFor(
                FoundFieldError.QUESTION_REQUIRED,
                FoundFieldError.QUESTION_TOO_LONG,
                FoundFieldError.TOO_MANY_QUESTIONS,
                FoundFieldError.ANSWER_REQUIRED,
                FoundFieldError.ANSWER_TOO_LONG
            )
        )
        if (state.canAddQuestion) {
            SecondaryButton(
                text = "Agregar pregunta",
                onClick = { onEvent(FoundFormEvent.OnAddQuestion) },
                leadingIcon = DsIcons.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun FieldError(message: String?) {
    if (message != null) AppLabel(message, color = UcbEncuentraTheme.colors.error)
}

private val QUESTION_EXAMPLES = listOf(
    "Ej. ¿Qué fondo de pantalla tiene?",
    "Ej. ¿Qué hay escrito en la funda?",
    "Ej. ¿Qué otro objeto estaba junto?"
)
