package bo.edu.ucb.encuentra.core.domain.model

import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Instant

/** Reporte de un objeto PERDIDO creado por un estudiante con el wizard (L02-L04). */
data class LostReport(
    val id: ItemId,
    val ownerId: String,
    val title: String,
    val category: Category,
    val subcategory: Subcategory,
    val color: String?,
    val brand: String?,
    val description: String,
    val campus: Campus,
    val zone: CampusZone,
    val locationDetail: String?,
    val lostFrom: Instant,
    val lostTo: Instant,
    val photoUrl: String?,
    val contactPhone: String?,
    val status: ReportStatus,
    val createdAt: Instant
)

/** Objeto ENCONTRADO registrado por staff (H03), en custodia hasta [retentionUntil] (D04). */
data class FoundItem(
    val id: ItemId,
    val title: String,
    val category: Category,
    val subcategory: Subcategory,
    val color: String?,
    val brand: String?,
    val description: String,
    val campus: Campus,
    val foundZone: CampusZone,
    val foundDetail: String?,
    val foundAt: Instant,
    val custodyPlace: CustodyPlace,
    val custodyDetail: String,
    val status: ItemStatus,
    val photoUrls: List<String>,
    /** 1-3 preguntas de control que el dueño debe responder (V01). */
    val controlQuestions: List<String>,
    val registeredBy: String,
    val retentionUntil: Instant,
    val createdAt: Instant,
    /** Respuesta esperada de cada pregunta (misma posición). PRIVADA: solo la ve el staff (V01). */
    val expectedAnswers: List<String> = emptyList()
)

/** Un movimiento en el historial de custodia de un objeto encontrado (H05). */
data class CustodyEvent(
    val id: ItemId,
    val foundItemId: ItemId,
    val place: CustodyPlace,
    val detail: String,
    val changedBy: String,
    val at: Instant,
    val note: String?
)

/** Coincidencia entre un reporte de perdido y un objeto encontrado, con score 0-100 (M01/M02). */
data class Match(
    val id: ItemId,
    val lostReportId: ItemId,
    val foundItemId: ItemId,
    val score: Int,
    val status: MatchStatus,
    val createdAt: Instant
) {
    init {
        require(score in MIN_SCORE..MAX_SCORE) { "El score debe estar entre $MIN_SCORE y $MAX_SCORE" }
    }

    companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
    }
}

/** Reclamo de un objeto encontrado ("Esto es mío — verificar", V02). [number] se muestra como "Reclamo #1024". */
data class Claim(
    val id: ItemId,
    val number: Int,
    val foundItemId: ItemId,
    val matchId: ItemId?,
    val claimantId: String,
    val status: ClaimStatus,
    /** Respuestas a las preguntas de control: privadas, solo las ve staff (V01). */
    val controlAnswers: List<String>,
    val createdAt: Instant,
    val reviewedBy: String?,
    val reviewedAt: Instant?,
    val scheduledDeliveryAt: Instant?,
    val deliveredBy: String?,
    val deliveredAt: Instant?,
    /** Envíos de respuestas que no coincidieron (V01): al llegar al límite el reclamo se bloquea. */
    val failedAttempts: Int = 0
) {
    val attemptsLeft: Int get() = (MAX_ANSWER_ATTEMPTS - failedAttempts).coerceAtLeast(0)

    companion object {
        /** Intentos para responder las preguntas de control antes de que el reclamo se bloquee (V01). */
        const val MAX_ANSWER_ATTEMPTS = 3
    }
}

/** Evento del registro de auditoría: solo se agrega, nunca se edita ni borra (S05). */
data class AuditLog(
    val id: Long,
    val action: AuditAction,
    val actorId: String,
    val targetId: String,
    val detail: String,
    val at: Instant
)

/** Notificación interna guardada en Room (N01). [deepLink] abre la pantalla correcta (F05). */
data class AppNotification(
    val id: ItemId,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val deepLink: String?,
    val read: Boolean,
    val createdAt: Instant
)
