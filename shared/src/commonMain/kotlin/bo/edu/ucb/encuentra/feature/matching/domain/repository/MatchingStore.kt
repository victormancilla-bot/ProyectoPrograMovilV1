package bo.edu.ucb.encuentra.feature.matching.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/** Lo que el matching lee y escribe. Separado de Room para poder probar el flujo completo con fakes. */
interface MatchingStore {
    suspend fun availableFoundItems(): List<FoundItem>

    suspend fun searchingReports(): List<LostReport>

    /** Todas las coincidencias de un reporte, en cualquier estado (incluye las "No es mío"). */
    suspend fun matchesForReport(reportId: ItemId): List<Match>

    /** Crea o actualiza (mismo id) estas coincidencias. */
    suspend fun saveMatches(matches: List<Match>)

    suspend fun deleteMatches(ids: List<ItemId>)

    suspend fun notify(notifications: List<AppNotification>)
}
