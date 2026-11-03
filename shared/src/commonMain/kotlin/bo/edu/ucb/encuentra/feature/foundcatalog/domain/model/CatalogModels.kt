package bo.edu.ucb.encuentra.feature.foundcatalog.domain.model

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import kotlin.time.Instant

/**
 * Un objeto del catálogo público (L07): SOLO categoría, tipo, zona y fecha (+ foto difuminada). No lleva el título
 * que escribió el staff, la descripción, el color, la marca, la custodia ni las preguntas de control: con eso
 * cualquiera podría reclamar algo ajeno. Para reclamarlo, el dueño lo reporta y el matching lo conecta (M01-M05).
 */
data class CatalogItem(
    val id: String,
    val category: Category,
    val subcategory: Subcategory,
    val zone: CampusZone,
    val foundAt: Instant,
    val photoUrl: String?
)

/** Filtros del catálogo: null = todas. */
data class CatalogFilter(
    val category: Category? = null,
    val zone: CampusZone? = null
) {
    fun matches(item: CatalogItem): Boolean =
        (category == null || item.category == category) && (zone == null || item.zone == zone)
}
