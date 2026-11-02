package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import bo.edu.ucb.encuentra.core.domain.model.Category
import edu.ucb.ds.components.selection.DefaultItemColors
import edu.ucb.ds.components.selection.SwatchOption

/** Ícono de cada categoría (Figma "Reportar - Categoría"); lo usan el wizard (L02) y el registro de hallazgos (H03). */
fun Category.icon(): ImageVector = when (this) {
    Category.ELECTRONICA -> Icons.Filled.Devices
    Category.ROPA_ACCESORIOS -> Icons.Filled.Checkroom
    Category.DOCUMENTOS -> Icons.Filled.Description
    Category.LIBROS_CUADERNOS -> Icons.Filled.MenuBook
    Category.LLAVES -> Icons.Filled.Key
    Category.OTROS -> Icons.Filled.MoreHoriz
}

/** Colores extra que aparecen con el "+" del selector (los 5 de [DefaultItemColors] son los del Figma). */
val ExtraItemColors = listOf(
    SwatchOption("Plateado", Color(0xFFC0C0C0)),
    SwatchOption("Dorado", Color(0xFFD4AF37)),
    SwatchOption("Verde", Color(0xFF16A34A)),
    SwatchOption("Amarillo", Color(0xFFEAB308)),
    SwatchOption("Naranja", Color(0xFFEA580C)),
    SwatchOption("Rosado", Color(0xFFEC4899)),
    SwatchOption("Morado", Color(0xFF7C3AED)),
    SwatchOption("Café", Color(0xFF78350F))
)

/** Paleta completa: los nombres son los que se guardan en `color` y compara el matching (M01). */
val AllItemColors = DefaultItemColors + ExtraItemColors

fun swatchFor(colorName: String?): SwatchOption? = AllItemColors.firstOrNull { it.name == colorName }
