package bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportDraftState
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import edu.ucb.ds.components.cards.CategoryGrid
import edu.ucb.ds.components.cards.CategoryOption
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Ícono de cada categoría en la grilla (Figma "Reportar - Categoría"). */
internal fun Category.icon(): ImageVector = when (this) {
    Category.ELECTRONICA -> Icons.Filled.Devices
    Category.ROPA_ACCESORIOS -> Icons.Filled.Checkroom
    Category.DOCUMENTOS -> Icons.Filled.Description
    Category.LIBROS_CUADERNOS -> Icons.Filled.MenuBook
    Category.LLAVES -> Icons.Filled.Key
    Category.OTROS -> Icons.Filled.MoreHoriz
}

/** Paso 1: grilla de categorías y, al elegir una, sus subcategorías (la confirmación muestra "Electrónica / Laptops"). */
@Composable
internal fun CategoryStep(state: ReportDraftState, onEvent: (ReportWizardEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val draft = state.draft
    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            AppHeading("¿Qué perdiste?", textAlign = TextAlign.Center)
            AppBody(
                "Selecciona la categoría que mejor describe el objeto que estás reportando.",
                textAlign = TextAlign.Center
            )
        }

        CategoryGrid(
            options = Category.entries.map { CategoryOption(it.name, it.label, it.icon()) },
            selectedId = draft.category?.name,
            onSelect = { option -> onEvent(ReportWizardEvent.OnCategorySelected(Category.valueOf(option.id))) }
        )

        val category = draft.category
        if (category != null) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                AppOverline("¿Qué tipo de ${category.label.lowercase()}?")
                val subcategories = category.subcategories
                AppChipGroup(
                    options = subcategories.map { it.label },
                    selected = draft.subcategory?.label.orEmpty(),
                    onSelect = { label ->
                        subcategories.firstOrNull { it.label == label }
                            ?.let { onEvent(ReportWizardEvent.OnSubcategorySelected(it)) }
                    }
                )
            }
        }
    }
}
