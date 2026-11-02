package bo.edu.ucb.encuentra.feature.matching.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match

/** Lo que el matching (M01) lee y escribe. Separado de Room para poder probar el flujo completo con fakes. */
interface MatchingStore {
    suspend fun availableFoundItems(): List<FoundItem>

    suspend fun searchingReports(): List<LostReport>

    /** Guarda solo los pares que todavía no existen y devuelve esos (los nuevos). */
    suspend fun saveNewMatches(matches: List<Match>): List<Match>

    suspend fun notify(notifications: List<AppNotification>)
}
