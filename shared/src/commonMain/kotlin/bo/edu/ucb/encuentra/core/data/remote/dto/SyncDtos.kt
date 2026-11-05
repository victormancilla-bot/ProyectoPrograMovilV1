package bo.edu.ucb.encuentra.core.data.remote.dto

import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import kotlinx.serialization.Serializable

// L06 · Documentos de Firestore (DTO) ↔ tablas de Room (Entity).
// - El id del documento es el mismo id de Room (no va dentro del documento).
// - Fechas en epoch millis y enums por nombre, igual que en Room: así un documento escrito por otra versión de la
//   app con un valor que esta no conoce se ignora (toEntity devuelve null) en vez de cerrar la app.

/** `lost_reports/{id}`. */
@Serializable
data class LostReportDto(
    val ownerId: String = "",
    val title: String = "",
    val category: String = "",
    val subcategory: String = "",
    val color: String? = null,
    val brand: String? = null,
    val description: String = "",
    val campus: String = "",
    val zone: String = "",
    val locationDetail: String? = null,
    val lostFrom: Long = 0,
    val lostTo: Long = 0,
    val photoUrl: String? = null,
    val contactPhone: String? = null,
    val status: String = "",
    val createdAt: Long = 0
)

/**
 * `found_items/{id}`. Sin `expectedAnswers`: las respuestas esperadas de las preguntas de control son privadas del
 * staff y el estudiante puede leer este documento (catálogo y matching). Se quedan solo en el teléfono que las
 * registró (L06b decide dónde se verifican).
 */
@Serializable
data class FoundItemDto(
    val title: String = "",
    val category: String = "",
    val subcategory: String = "",
    val color: String? = null,
    val brand: String? = null,
    val description: String = "",
    val campus: String = "",
    val foundZone: String = "",
    val foundDetail: String? = null,
    val foundAt: Long = 0,
    val custodyPlace: String = "",
    val custodyDetail: String = "",
    val status: String = "",
    val photoUrls: List<String> = emptyList(),
    val controlQuestions: List<String> = emptyList(),
    val registeredBy: String = "",
    val retentionUntil: Long = 0,
    val createdAt: Long = 0
)

fun LostReportEntity.toDto() = LostReportDto(
    ownerId = ownerId,
    title = title,
    category = category.name,
    subcategory = subcategory.name,
    color = color,
    brand = brand,
    description = description,
    campus = campus.name,
    zone = zone.name,
    locationDetail = locationDetail,
    lostFrom = lostFrom,
    lostTo = lostTo,
    photoUrl = photoUrl,
    contactPhone = contactPhone,
    status = status.name,
    createdAt = createdAt
)

/** null si el documento está incompleto o trae un valor que esta versión no conoce. */
fun LostReportDto.toEntity(id: String): LostReportEntity? = parsing {
    LostReportEntity(
        id = id,
        ownerId = ownerId.ifBlank { return null },
        title = title,
        category = Category.valueOf(category),
        subcategory = Subcategory.valueOf(subcategory),
        color = color,
        brand = brand,
        description = description,
        campus = Campus.valueOf(campus),
        zone = CampusZone.valueOf(zone),
        locationDetail = locationDetail,
        lostFrom = lostFrom,
        lostTo = lostTo,
        photoUrl = photoUrl,
        contactPhone = contactPhone,
        status = ReportStatus.valueOf(status),
        createdAt = createdAt
    )
}

fun FoundItemEntity.toDto() = FoundItemDto(
    title = title,
    category = category.name,
    subcategory = subcategory.name,
    color = color,
    brand = brand,
    description = description,
    campus = campus.name,
    foundZone = foundZone.name,
    foundDetail = foundDetail,
    foundAt = foundAt,
    custodyPlace = custodyPlace.name,
    custodyDetail = custodyDetail,
    status = status.name,
    photoUrls = photoUrls,
    controlQuestions = controlQuestions,
    registeredBy = registeredBy,
    retentionUntil = retentionUntil,
    createdAt = createdAt
)

/**
 * null si el documento está incompleto o trae un valor desconocido. [local] es la fila actual en Room: de ella se
 * conservan las respuestas esperadas, que no viajan a Firestore.
 */
fun FoundItemDto.toEntity(id: String, local: FoundItemEntity? = null): FoundItemEntity? = parsing {
    FoundItemEntity(
        id = id,
        title = title,
        category = Category.valueOf(category),
        subcategory = Subcategory.valueOf(subcategory),
        color = color,
        brand = brand,
        description = description,
        campus = Campus.valueOf(campus),
        foundZone = CampusZone.valueOf(foundZone),
        foundDetail = foundDetail,
        foundAt = foundAt,
        custodyPlace = CustodyPlace.valueOf(custodyPlace),
        custodyDetail = custodyDetail,
        status = ItemStatus.valueOf(status),
        photoUrls = photoUrls,
        controlQuestions = controlQuestions,
        registeredBy = registeredBy.ifBlank { return null },
        retentionUntil = retentionUntil,
        createdAt = createdAt,
        expectedAnswers = local?.expectedAnswers.orEmpty()
    )
}

/** `valueOf` lanza IllegalArgumentException con un nombre desconocido: ese documento se ignora. */
private inline fun <T> parsing(block: () -> T): T? =
    try {
        block()
    } catch (_: IllegalArgumentException) {
        null
    }
