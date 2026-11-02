package bo.edu.ucb.encuentra.feature.claims.domain.usecase

import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerComparator
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerReview
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimWithItem
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Detalle de un reclamo para el staff (V01; lo muestra el detalle de V03): cada pregunta con la respuesta del
 * estudiante, la esperada y la sugerencia "coincide / no coincide". Las respuestas son PRIVADAS: si quien pide
 * no es staff, no se devuelve nada.
 */
class ObserveClaimReviewUseCase(
    private val repository: ClaimRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(claimId: ItemId): Flow<ClaimReview?> {
        if (getCurrentUser()?.role?.isStaff != true) return flowOf(null)
        return repository.observeWithItem(claimId).map { it?.toReview() }
    }
}

/** Reclamos pendientes o en verificación para la lista del staff. Vacía si quien pide no es staff. */
class ObserveOpenClaimsUseCase(
    private val repository: ClaimRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<ClaimReview>> {
        if (getCurrentUser()?.role?.isStaff != true) return flowOf(emptyList())
        return repository.observeOpen().map { claims -> claims.map { it.toReview() } }
    }
}

internal fun ClaimWithItem.toReview(): ClaimReview {
    val answered = claim.controlAnswers.size == item.controlQuestions.size
    return ClaimReview(
        claim = claim,
        item = item,
        claimantName = claimantName,
        answers = item.controlQuestions.mapIndexed { i, question ->
            val answer = claim.controlAnswers.getOrNull(i)?.takeIf { answered }
            val expected = item.expectedAnswers.getOrNull(i)
            AnswerReview(
                question = question,
                answer = answer,
                expected = expected,
                verdict = answer?.let { AnswerComparator.compare(it, expected) }
            )
        },
        report = report,
        matchScore = matchScore
    )
}
