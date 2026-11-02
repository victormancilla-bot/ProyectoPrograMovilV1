package bo.edu.ucb.encuentra.core.data.local.mapper

import bo.edu.ucb.encuentra.core.data.local.entity.AppNotificationEntity
import bo.edu.ucb.encuentra.core.data.local.entity.AuditLogEntity
import bo.edu.ucb.encuentra.core.data.local.entity.ClaimEntity
import bo.edu.ucb.encuentra.core.data.local.entity.CustodyEventEntity
import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.data.local.entity.MatchEntity
import bo.edu.ucb.encuentra.core.data.local.entity.UserEntity
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.AuditLog
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Instant

// Entity (Room) ↔ modelo de dominio. Las fechas se guardan en epoch millis.

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
private fun Instant.toMillis(): Long = toEpochMilliseconds()

fun UserEntity.toModel() = UserModel(
    id = id,
    fullName = fullName,
    email = Email(email),
    role = role,
    campus = campus,
    faculty = faculty
)

fun UserModel.toEntity() = UserEntity(
    id = id,
    fullName = fullName,
    email = email.value,
    role = role,
    campus = campus,
    faculty = faculty
)

fun LostReportEntity.toModel() = LostReport(
    id = ItemId(id),
    ownerId = ownerId,
    title = title,
    category = category,
    subcategory = subcategory,
    color = color,
    brand = brand,
    description = description,
    campus = campus,
    zone = zone,
    locationDetail = locationDetail,
    lostFrom = lostFrom.toInstant(),
    lostTo = lostTo.toInstant(),
    photoUrl = photoUrl,
    contactPhone = contactPhone,
    status = status,
    createdAt = createdAt.toInstant()
)

fun LostReport.toEntity() = LostReportEntity(
    id = id.value,
    ownerId = ownerId,
    title = title,
    category = category,
    subcategory = subcategory,
    color = color,
    brand = brand,
    description = description,
    campus = campus,
    zone = zone,
    locationDetail = locationDetail,
    lostFrom = lostFrom.toMillis(),
    lostTo = lostTo.toMillis(),
    photoUrl = photoUrl,
    contactPhone = contactPhone,
    status = status,
    createdAt = createdAt.toMillis()
)

fun FoundItemEntity.toModel() = FoundItem(
    id = ItemId(id),
    title = title,
    category = category,
    subcategory = subcategory,
    color = color,
    brand = brand,
    description = description,
    campus = campus,
    foundZone = foundZone,
    foundDetail = foundDetail,
    foundAt = foundAt.toInstant(),
    custodyPlace = custodyPlace,
    custodyDetail = custodyDetail,
    status = status,
    photoUrls = photoUrls,
    controlQuestions = controlQuestions,
    registeredBy = registeredBy,
    retentionUntil = retentionUntil.toInstant(),
    createdAt = createdAt.toInstant()
)

fun FoundItem.toEntity() = FoundItemEntity(
    id = id.value,
    title = title,
    category = category,
    subcategory = subcategory,
    color = color,
    brand = brand,
    description = description,
    campus = campus,
    foundZone = foundZone,
    foundDetail = foundDetail,
    foundAt = foundAt.toMillis(),
    custodyPlace = custodyPlace,
    custodyDetail = custodyDetail,
    status = status,
    photoUrls = photoUrls,
    controlQuestions = controlQuestions,
    registeredBy = registeredBy,
    retentionUntil = retentionUntil.toMillis(),
    createdAt = createdAt.toMillis()
)

fun CustodyEventEntity.toModel() = CustodyEvent(
    id = ItemId(id),
    foundItemId = ItemId(foundItemId),
    place = place,
    detail = detail,
    changedBy = changedBy,
    at = at.toInstant(),
    note = note
)

fun CustodyEvent.toEntity() = CustodyEventEntity(
    id = id.value,
    foundItemId = foundItemId.value,
    place = place,
    detail = detail,
    changedBy = changedBy,
    at = at.toMillis(),
    note = note
)

fun MatchEntity.toModel() = Match(
    id = ItemId(id),
    lostReportId = ItemId(lostReportId),
    foundItemId = ItemId(foundItemId),
    score = score,
    status = status,
    createdAt = createdAt.toInstant()
)

fun Match.toEntity() = MatchEntity(
    id = id.value,
    lostReportId = lostReportId.value,
    foundItemId = foundItemId.value,
    score = score,
    status = status,
    createdAt = createdAt.toMillis()
)

fun ClaimEntity.toModel() = Claim(
    id = ItemId(id),
    number = number,
    foundItemId = ItemId(foundItemId),
    matchId = matchId?.let(::ItemId),
    claimantId = claimantId,
    status = status,
    controlAnswers = controlAnswers,
    createdAt = createdAt.toInstant(),
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt?.toInstant(),
    scheduledDeliveryAt = scheduledDeliveryAt?.toInstant(),
    deliveredBy = deliveredBy,
    deliveredAt = deliveredAt?.toInstant()
)

fun Claim.toEntity() = ClaimEntity(
    id = id.value,
    number = number,
    foundItemId = foundItemId.value,
    matchId = matchId?.value,
    claimantId = claimantId,
    status = status,
    controlAnswers = controlAnswers,
    createdAt = createdAt.toMillis(),
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt?.toMillis(),
    scheduledDeliveryAt = scheduledDeliveryAt?.toMillis(),
    deliveredBy = deliveredBy,
    deliveredAt = deliveredAt?.toMillis()
)

fun AuditLogEntity.toModel() = AuditLog(
    id = id,
    action = action,
    actorId = actorId,
    targetId = targetId,
    detail = detail,
    at = at.toInstant()
)

/** id = 0 deja que Room lo autogenere. */
fun AuditLog.toEntity() = AuditLogEntity(
    id = id,
    action = action,
    actorId = actorId,
    targetId = targetId,
    detail = detail,
    at = at.toMillis()
)

fun AppNotificationEntity.toModel() = AppNotification(
    id = ItemId(id),
    userId = userId,
    type = type,
    title = title,
    body = body,
    deepLink = deepLink,
    read = read,
    createdAt = createdAt.toInstant()
)

fun AppNotification.toEntity() = AppNotificationEntity(
    id = id.value,
    userId = userId,
    type = type,
    title = title,
    body = body,
    deepLink = deepLink,
    read = read,
    createdAt = createdAt.toMillis()
)
