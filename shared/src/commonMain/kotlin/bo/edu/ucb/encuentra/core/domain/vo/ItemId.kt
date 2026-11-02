package bo.edu.ucb.encuentra.core.domain.vo

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Identificador de reportes, objetos encontrados, matches y reclamos.
 * Es texto (no Int autogenerado) para que el mismo id sirva en Room y en Firestore (L06).
 */
@JvmInline
value class ItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ItemId no puede estar vacío" }
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generate(): ItemId = ItemId(Uuid.random().toString())
    }
}
