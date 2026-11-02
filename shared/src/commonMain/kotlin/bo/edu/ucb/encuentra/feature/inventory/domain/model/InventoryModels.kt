package bo.edu.ucb.encuentra.feature.inventory.domain.model

import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.RetentionState
import bo.edu.ucb.encuentra.core.domain.model.code

/** Chips del mockup "Inventario": Todos / Disponible / Reclamado / Vencido. */
enum class InventoryFilter(val label: String) {
    TODOS("Todos"),
    DISPONIBLE("Disponible"),
    RECLAMADO("Reclamado"),
    VENCIDO("Vencido")
}

/** Un objeto del inventario con su estado de retención calculado a la hora actual. */
data class InventoryEntry(val item: FoundItem, val retention: RetentionState)

/** Un movimiento del historial de custodia con el nombre de quién lo hizo. */
data class CustodyEntry(val event: CustodyEvent, val changedByName: String?)

/** Detalle del objeto para el staff (H07): datos completos, retención e historial de custodia. */
data class InventoryItemDetail(
    val entry: InventoryEntry,
    val registeredByName: String?,
    val history: List<CustodyEntry>
) {
    /** Solo se puede mover lo que sigue físicamente en Objetos Perdidos (H05). */
    val canEditCustody: Boolean
        get() = entry.item.status == ItemStatus.DISPONIBLE || entry.item.status == ItemStatus.RECLAMADO
}

/**
 * Filtro y búsqueda del inventario. La búsqueda ignora mayúsculas y tildes y mira nombre, código, marca, color,
 * descripción, categoría, subcategoría y lugar ("casio", "#CALCULAD", "bloque c").
 * Orden: primero lo que requiere atención (vencidos, luego por vencer) y después lo más reciente.
 */
object InventorySearch {

    fun apply(entries: List<InventoryEntry>, filter: InventoryFilter, query: String): List<InventoryEntry> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        return entries
            .filter { it.matches(filter) }
            .filter { entry -> words.isEmpty() || haystack(entry.item).let { text -> words.all { it in text } } }
            .sortedWith(compareBy<InventoryEntry> { ATTENTION.indexOf(it.retention) }.thenByDescending { it.item.foundAt })
    }

    private fun InventoryEntry.matches(filter: InventoryFilter): Boolean = when (filter) {
        InventoryFilter.TODOS -> true
        // "Disponible" son los que están a la espera de su dueño y todavía no vencieron.
        InventoryFilter.DISPONIBLE -> item.status == ItemStatus.DISPONIBLE && retention != RetentionState.VENCIDO
        InventoryFilter.RECLAMADO -> item.status == ItemStatus.RECLAMADO
        InventoryFilter.VENCIDO -> retention == RetentionState.VENCIDO
    }

    private fun haystack(item: FoundItem): String = normalize(
        listOfNotNull(
            item.title, item.code, item.id.value, item.brand, item.color, item.description,
            item.category.label, item.subcategory.label, item.foundZone.label, item.foundDetail
        ).joinToString(" ")
    )

    internal fun normalize(text: String): String =
        text.lowercase().map { ACCENTS[it] ?: it }.joinToString("").replace(Regex("[^a-z0-9ñ#]+"), " ").trim()

    private val ATTENTION = listOf(RetentionState.VENCIDO, RetentionState.POR_VENCER, RetentionState.VIGENTE)
    private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')
}
