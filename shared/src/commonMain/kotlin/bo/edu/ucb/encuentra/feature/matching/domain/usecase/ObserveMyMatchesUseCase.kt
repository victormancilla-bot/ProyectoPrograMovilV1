package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Posibles coincidencias del usuario (M04): solo reportes con candidatos, cada uno ordenado de mayor a menor score.
 * Primero los reportes con coincidencias por revisar (PROPUESTO), después los ya confirmados.
 */
class ObserveMyMatchesUseCase(
    private val repository: MatchRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<ReportMatches>> {
        val user = getCurrentUser() ?: return flowOf(emptyList())
        return repository.observeForOwner(user.id).map { groups ->
            groups
                .map { group ->
                    group.copy(
                        candidates = group.candidates
                            .filter { it.match.status != MatchStatus.DESCARTADO }
                            .sortedByDescending { it.match.score }
                    )
                }
                .filter { it.candidates.isNotEmpty() }
                .sortedWith(
                    compareByDescending<ReportMatches> { group ->
                        group.candidates.any { it.match.status == MatchStatus.PROPUESTO }
                    }.thenByDescending { group -> group.candidates.first().match.score }
                )
        }
    }
}
