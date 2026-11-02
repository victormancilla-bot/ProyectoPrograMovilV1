package bo.edu.ucb.encuentra.feature.matching.domain.repository

import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig

interface MatchingConfigRepository {
    /** Pesos y umbrales vigentes; nunca falla: sin Remote Config devuelve [MatchingConfig.DEFAULT]. */
    suspend fun get(): MatchingConfig
}
