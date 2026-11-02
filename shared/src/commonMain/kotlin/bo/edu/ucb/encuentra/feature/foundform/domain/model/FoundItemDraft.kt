package bo.edu.ucb.encuentra.feature.foundform.domain.model

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Borrador del formulario "Registrar Objeto" (H03), lo llena el staff.
 * [suggestedByAi] marca que categoría y color vinieron de la IA (H02); en R3 se eligen a mano.
 */
data class FoundItemDraft(
    // Datos detectados (editables)
    val category: Category? = null,
    val subcategory: Subcategory? = null,
    val color: String? = null,
    val suggestedByAi: Boolean = false,
    // Detalle
    val title: String = "",
    val brand: String = "",
    val features: String = "",
    // Dónde, cuándo y custodia
    val foundZone: CampusZone? = null,
    val custodyPlace: CustodyPlace? = null,
    val custodyDetail: String = "",
    val foundDate: LocalDate? = null,
    val foundTime: LocalTime? = null,
    /** 1-3 preguntas que solo el dueño puede responder (V01). */
    val controlQuestions: List<String> = listOf("")
)
