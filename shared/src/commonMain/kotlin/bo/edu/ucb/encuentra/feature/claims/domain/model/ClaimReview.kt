package bo.edu.ucb.encuentra.feature.claims.domain.model

import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.FoundItem

/** Un reclamo con su objeto y el nombre de quien reclama, tal como sale de Room. */
data class ClaimWithItem(val claim: Claim, val item: FoundItem, val claimantName: String?)

/** Una pregunta de control con lo que respondió el estudiante, lo esperado y la sugerencia de la app (V01). */
data class AnswerReview(
    val question: String,
    /** null si todavía no respondió. */
    val answer: String?,
    val expected: String?,
    /** null si todavía no respondió. */
    val verdict: AnswerVerdict?
)

/**
 * Detalle de un reclamo para el staff (V01 → se muestra en el detalle de V03). Contiene datos PRIVADOS
 * (respuestas y respuestas esperadas): solo lo arma [bo.edu.ucb.encuentra.feature.claims.domain.usecase.ObserveClaimReviewUseCase]
 * y solo para el staff.
 */
data class ClaimReview(
    val claim: Claim,
    val item: FoundItem,
    val claimantName: String?,
    val answers: List<AnswerReview>
) {
    val matchingAnswers: Int get() = answers.count { it.verdict == AnswerVerdict.COINCIDE }

    /** La decisión es humana: el staff aprueba solo lo que ya respondió las preguntas. */
    val canApprove: Boolean get() = claim.status == ClaimStatus.EN_VERIFICACION
    val canReject: Boolean get() = claim.status == ClaimStatus.PENDIENTE || claim.status == ClaimStatus.EN_VERIFICACION
}
