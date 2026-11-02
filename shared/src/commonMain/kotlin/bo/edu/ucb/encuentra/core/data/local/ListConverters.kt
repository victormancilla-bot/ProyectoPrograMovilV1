package bo.edu.ucb.encuentra.core.data.local

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

/**
 * Room no guarda listas: las convierte a texto JSON con kotlinx.serialization (fotos, preguntas, respuestas).
 * Los enums no necesitan conversor: room3 los guarda por nombre automáticamente.
 */
object ListConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @ColumnTypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @ColumnTypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
