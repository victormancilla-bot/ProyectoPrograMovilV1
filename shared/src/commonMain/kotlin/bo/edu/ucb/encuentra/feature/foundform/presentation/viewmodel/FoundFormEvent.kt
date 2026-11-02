package bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed class FoundFormEvent {
    // Datos detectados (editables; en R3 se eligen a mano, la IA llega con H02)
    data class OnCategorySelected(val category: Category) : FoundFormEvent()
    data class OnSubcategorySelected(val subcategory: Subcategory) : FoundFormEvent()
    data class OnColorSelected(val color: String) : FoundFormEvent()
    data object OnShowAllColors : FoundFormEvent()

    // Detalle
    data class OnTitleChanged(val title: String) : FoundFormEvent()
    data class OnBrandChanged(val brand: String) : FoundFormEvent()
    data class OnFeaturesChanged(val features: String) : FoundFormEvent()

    // Dónde, cuándo y custodia
    data class OnFoundZoneSelected(val zone: CampusZone) : FoundFormEvent()
    data class OnCustodySelected(val place: CustodyPlace) : FoundFormEvent()
    data class OnCustodyDetailChanged(val detail: String) : FoundFormEvent()
    data class OnFoundDateSelected(val date: LocalDate) : FoundFormEvent()
    data class OnFoundTimeSelected(val time: LocalTime) : FoundFormEvent()

    // Preguntas de control (V01)
    data class OnQuestionChanged(val index: Int, val text: String) : FoundFormEvent()
    /** Respuesta esperada de la pregunta [index] (V01, privada). */
    data class OnExpectedAnswerChanged(val index: Int, val text: String) : FoundFormEvent()
    data object OnAddQuestion : FoundFormEvent()
    data class OnRemoveQuestion(val index: Int) : FoundFormEvent()

    data object OnSubmit : FoundFormEvent()
    data object OnDismissError : FoundFormEvent()
}
