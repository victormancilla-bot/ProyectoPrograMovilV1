package bo.edu.ucb.encuentra.feature.reportwizard.domain.model

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory

/**
 * Borrador del reporte de objeto perdido, compartido por los 4 pasos del wizard.
 * Vive solo en el ViewModel: si el wizard se cancela, se descarta (F05).
 * L03 agrega dónde y cuándo, foto y contacto; L04 lo envía.
 */
data class ReportDraft(
    // Paso 1 · Categoría
    val category: Category? = null,
    val subcategory: Subcategory? = null,
    // Paso 2 · Atributos
    val title: String = "",
    val color: String? = null,
    val brand: String = "",
    val features: String = ""
)
