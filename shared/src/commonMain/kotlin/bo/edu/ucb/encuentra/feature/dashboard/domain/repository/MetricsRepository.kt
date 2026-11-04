package bo.edu.ucb.encuentra.feature.dashboard.domain.repository

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ItemOutcome
import kotlinx.coroutines.flow.Flow

/** D02 · Objetos encontrados con su salida de custodia, en vivo. */
interface MetricsRepository {
    fun observeOutcomes(): Flow<List<ItemOutcome>>
}
