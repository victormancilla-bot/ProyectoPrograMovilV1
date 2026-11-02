package bo.edu.ucb.encuentra.feature.reportwizard.domain.model

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Borrador del reporte de objeto perdido, compartido por los 4 pasos del wizard.
 * Vive solo en el ViewModel: si el wizard se cancela, se descarta (F05). L04 lo envía.
 */
data class ReportDraft(
    // Paso 1 · Categoría
    val category: Category? = null,
    val subcategory: Subcategory? = null,
    // Paso 2 · Atributos
    val title: String = "",
    val color: String? = null,
    val brand: String = "",
    val features: String = "",
    // Paso 3 · Dónde y cuándo (L03: rango de fechas, no una sola fecha como el mockup)
    val zone: CampusZone? = null,
    val locationDetail: String = "",
    val lostFrom: LocalDate? = null,
    val lostTo: LocalDate? = null,
    val timeSlot: TimeSlot? = null,
    /** Foto de referencia opcional: ruta local de la copia que guardó la app. */
    val photoUri: String? = null,
    /** Contacto: el correo viene del perfil; el teléfono es opcional. */
    val contactEmail: String = "",
    val contactPhone: String = ""
)

/** Franja horaria en la que se perdió (la confirmación muestra "15 de Noviembre - Mañana"). */
enum class TimeSlot(val label: String, val start: LocalTime, val end: LocalTime) {
    MANANA("Mañana", LocalTime(6, 0), LocalTime(12, 0)),
    TARDE("Tarde", LocalTime(12, 0), LocalTime(18, 0)),
    NOCHE("Noche", LocalTime(18, 0), LocalTime(23, 59)),
    NO_SE("No sé", LocalTime(0, 0), LocalTime(23, 59))
}
