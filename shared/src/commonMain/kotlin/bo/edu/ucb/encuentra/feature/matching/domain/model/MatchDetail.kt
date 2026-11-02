package bo.edu.ucb.encuentra.feature.matching.domain.model

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlin.time.Instant

/** Una coincidencia completa, tal como sale de Room (solo la usa el dominio para armar [MatchDetail]). */
data class MatchPair(val match: Match, val report: LostReport, val item: FoundItem, val ownClaim: Claim? = null)

/**
 * El reclamo que el dueño ya hizo sobre este objeto (V02): se muestra en vez de los botones.
 * Mientras está PENDIENTE trae las preguntas de control para responderlas (V01); nunca las respuestas esperadas.
 */
data class ExistingClaim(
    val id: ItemId,
    val number: Int,
    val status: ClaimStatus,
    val failedAttempts: Int = 0,
    val questions: List<String> = emptyList()
) {
    val attemptsLeft: Int get() = (Claim.MAX_ANSWER_ATTEMPTS - failedAttempts).coerceAtLeast(0)

    /** Rechazado por agotar los intentos (no por decisión del staff). */
    val blocked: Boolean get() = status == ClaimStatus.RECHAZADO && attemptsLeft == 0
}

/**
 * Vista LIMITADA del objeto encontrado (M05): lo justo para reconocerlo. NO incluye la custodia, las preguntas
 * de control, la descripción del staff ni el detalle exacto del lugar, porque podrían dar las respuestas a quien
 * no es el dueño. Esos datos solo los ve el staff (V01/V03).
 */
data class FoundItemPreview(
    val title: String,
    val subcategory: Subcategory,
    val color: String?,
    val brand: String?,
    val zone: CampusZone,
    val foundAt: Instant,
    val photoUrl: String?
)

/** ✔ = coincide con lo reportado · ? = no coincide o no se sabe: lo resuelve el staff al verificar. */
enum class ComparisonVerdict { COINCIDE, POR_VERIFICAR }

/** Tabla "Lo que reportaste" vs "Lo que se encontró". El tipo siempre coincide (es parte del filtro duro). */
data class MatchComparison(
    val type: ComparisonVerdict,
    val brand: ComparisonVerdict,
    val color: ComparisonVerdict,
    val zone: ComparisonVerdict,
    val date: ComparisonVerdict
)

/** Detalle de match (M05) para el dueño del reporte. */
data class MatchDetail(
    val matchId: ItemId,
    val score: Int,
    val status: MatchStatus,
    val report: LostReport,
    val item: FoundItemPreview,
    val comparison: MatchComparison,
    val existingClaim: ExistingClaim? = null
) {
    /**
     * "Esto es mío" y "No es mío" solo en coincidencias propuestas de un reporte abierto (buscando o con otro
     * reclamo en verificación) y sin reclamo previo de esta persona. Una confirmada ya la revisó el staff.
     */
    val canAct: Boolean get() = existingClaim == null && status == MatchStatus.PROPUESTO && report.status in ACTIONABLE

    private companion object {
        val ACTIONABLE = setOf(ReportStatus.BUSCANDO, ReportStatus.EN_VERIFICACION)
    }
}
