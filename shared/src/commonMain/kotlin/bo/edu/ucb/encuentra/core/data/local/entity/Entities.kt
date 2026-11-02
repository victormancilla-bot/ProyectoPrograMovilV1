package bo.edu.ucb.encuentra.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory

// Tablas de Room v1 (F06). Reglas:
// - Ids de texto: el mismo id sirve en Room y en Firestore (L06).
// - Fechas en epoch millis (Long); los mappers las convierten a kotlin.time.Instant.
// - Enums se guardan por nombre (TEXT). Las listas, como JSON (ListConverters).
// - Sin foreign keys: con sincronización de Firestore los datos pueden llegar en cualquier orden. Se usan índices.

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "campus") val campus: Campus,
    @ColumnInfo(name = "faculty") val faculty: String?
)

@Entity(
    tableName = "lost_reports",
    indices = [Index(value = ["owner_id"]), Index(value = ["status"])]
)
data class LostReportEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "category") val category: Category,
    @ColumnInfo(name = "subcategory") val subcategory: Subcategory,
    @ColumnInfo(name = "color") val color: String?,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "campus") val campus: Campus,
    @ColumnInfo(name = "zone") val zone: CampusZone,
    @ColumnInfo(name = "location_detail") val locationDetail: String?,
    @ColumnInfo(name = "lost_from") val lostFrom: Long,
    @ColumnInfo(name = "lost_to") val lostTo: Long,
    @ColumnInfo(name = "photo_url") val photoUrl: String?,
    @ColumnInfo(name = "contact_phone") val contactPhone: String?,
    @ColumnInfo(name = "status") val status: ReportStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "found_items",
    indices = [Index(value = ["status"]), Index(value = ["category"])]
)
data class FoundItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "category") val category: Category,
    @ColumnInfo(name = "subcategory") val subcategory: Subcategory,
    @ColumnInfo(name = "color") val color: String?,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "campus") val campus: Campus,
    @ColumnInfo(name = "found_zone") val foundZone: CampusZone,
    @ColumnInfo(name = "found_detail") val foundDetail: String?,
    @ColumnInfo(name = "found_at") val foundAt: Long,
    @ColumnInfo(name = "custody_place") val custodyPlace: CustodyPlace,
    @ColumnInfo(name = "custody_detail") val custodyDetail: String,
    @ColumnInfo(name = "status") val status: ItemStatus,
    @ColumnInfo(name = "photo_urls") val photoUrls: List<String>,
    @ColumnInfo(name = "control_questions") val controlQuestions: List<String>,
    @ColumnInfo(name = "registered_by") val registeredBy: String,
    @ColumnInfo(name = "retention_until") val retentionUntil: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    // v2 (V01): respuestas esperadas de las preguntas de control, privadas del staff.
    @ColumnInfo(name = "expected_answers", defaultValue = "[]") val expectedAnswers: List<String> = emptyList()
)

@Entity(
    tableName = "custody_events",
    indices = [Index(value = ["found_item_id"])]
)
data class CustodyEventEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "found_item_id") val foundItemId: String,
    @ColumnInfo(name = "place") val place: CustodyPlace,
    @ColumnInfo(name = "detail") val detail: String,
    @ColumnInfo(name = "changed_by") val changedBy: String,
    @ColumnInfo(name = "at") val at: Long,
    @ColumnInfo(name = "note") val note: String?
)

@Entity(
    tableName = "matches",
    indices = [
        Index(value = ["lost_report_id", "found_item_id"], unique = true),
        Index(value = ["found_item_id"])
    ]
)
data class MatchEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "lost_report_id") val lostReportId: String,
    @ColumnInfo(name = "found_item_id") val foundItemId: String,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "status") val status: MatchStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** El índice único (found_item_id, claimant_id) bloquea el doble reclamo de la misma persona (V02). */
@Entity(
    tableName = "claims",
    indices = [
        Index(value = ["number"], unique = true),
        Index(value = ["found_item_id", "claimant_id"], unique = true),
        Index(value = ["claimant_id"]),
        Index(value = ["status"])
    ]
)
data class ClaimEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "found_item_id") val foundItemId: String,
    @ColumnInfo(name = "match_id") val matchId: String?,
    @ColumnInfo(name = "claimant_id") val claimantId: String,
    @ColumnInfo(name = "status") val status: ClaimStatus,
    @ColumnInfo(name = "control_answers") val controlAnswers: List<String>,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "reviewed_by") val reviewedBy: String?,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long?,
    @ColumnInfo(name = "scheduled_delivery_at") val scheduledDeliveryAt: Long?,
    @ColumnInfo(name = "delivered_by") val deliveredBy: String?,
    @ColumnInfo(name = "delivered_at") val deliveredAt: Long?,
    // v2 (V01): envíos de respuestas que no coincidieron.
    @ColumnInfo(name = "failed_attempts", defaultValue = "0") val failedAttempts: Int = 0
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["actor_id"]), Index(value = ["target_id"]), Index(value = ["at"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "action") val action: AuditAction,
    @ColumnInfo(name = "actor_id") val actorId: String,
    @ColumnInfo(name = "target_id") val targetId: String,
    @ColumnInfo(name = "detail") val detail: String,
    @ColumnInfo(name = "at") val at: Long
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["user_id"])]
)
data class AppNotificationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "type") val type: NotificationType,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "deep_link") val deepLink: String?,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
