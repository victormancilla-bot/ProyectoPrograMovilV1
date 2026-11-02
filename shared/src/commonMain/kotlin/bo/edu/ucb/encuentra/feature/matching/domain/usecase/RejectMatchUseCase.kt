package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository

/**
 * "No es mío" (M02): la coincidencia queda DESCARTADO para siempre (el matching la excluye) y se
 * re-calcula el ranking del reporte para que entre el siguiente candidato del Top-N.
 */
class RejectMatchUseCase(
    private val repository: MatchRepository,
    private val matching: MatchingTrigger
) {
    suspend operator fun invoke(matchId: ItemId): Result<Unit> = runCatching {
        val report = repository.reject(matchId) ?: throw NoSuchElementException("La coincidencia no existe")
        runCatching { matching.onReportUpdated(report) }
    }
}
