package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory

sealed class ReportWizardEvent {
    // Paso 1 · Categoría
    data class OnCategorySelected(val category: Category) : ReportWizardEvent()
    data class OnSubcategorySelected(val subcategory: Subcategory) : ReportWizardEvent()

    // Paso 2 · Atributos
    data class OnTitleChanged(val title: String) : ReportWizardEvent()
    data class OnColorSelected(val color: String) : ReportWizardEvent()
    data object OnShowAllColors : ReportWizardEvent()
    data class OnBrandChanged(val brand: String) : ReportWizardEvent()
    data class OnFeaturesChanged(val features: String) : ReportWizardEvent()

    // Navegación entre pasos
    data object OnNext : ReportWizardEvent()

    /** Flecha atrás del header: paso anterior, o cancelar el wizard si está en el paso 1. */
    data object OnBack : ReportWizardEvent()
}
