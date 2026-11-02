package bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.TimeSlot
import kotlinx.datetime.LocalDate

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

    // Paso 3 · Dónde y cuándo
    data class OnZoneSelected(val zone: CampusZone) : ReportWizardEvent()
    data class OnLocationDetailChanged(val detail: String) : ReportWizardEvent()
    data class OnLostFromSelected(val date: LocalDate) : ReportWizardEvent()
    data class OnLostToSelected(val date: LocalDate) : ReportWizardEvent()
    data class OnTimeSlotSelected(val slot: TimeSlot) : ReportWizardEvent()
    /** [uri] = copia local de la foto elegida en la galería. */
    data class OnPhotoPicked(val uri: String) : ReportWizardEvent()
    data object OnPhotoRemoved : ReportWizardEvent()
    data class OnPhoneChanged(val phone: String) : ReportWizardEvent()

    // Paso 4 · Confirmación y envío
    /** "Editar" de una sección del resumen: vuelve a ese paso (1-3). */
    data class OnEditStep(val step: Int) : ReportWizardEvent()
    data object OnSubmit : ReportWizardEvent()
    data object OnDismissSubmitError : ReportWizardEvent()

    // Pantalla de éxito
    data object OnGoToMyReports : ReportWizardEvent()
    data object OnFinish : ReportWizardEvent()

    // Navegación entre pasos
    data object OnNext : ReportWizardEvent()

    /** Flecha atrás del header: paso anterior, o cancelar el wizard si está en el paso 1. */
    data object OnBack : ReportWizardEvent()
}
