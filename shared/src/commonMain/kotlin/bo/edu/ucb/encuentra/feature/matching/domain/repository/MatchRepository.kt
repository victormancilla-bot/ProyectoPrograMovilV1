package bo.edu.ucb.encuentra.feature.matching.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.model.ReportMatches
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    /** Reportes de [ownerId] con sus coincidencias no descartadas. Se actualiza solo cuando cambia Room. */
    fun observeForOwner(ownerId: String): Flow<List<ReportMatches>>

    /** "No es mío": la coincidencia pasa a DESCARTADO y no vuelve a proponerse. Devuelve su reporte. */
    suspend fun reject(matchId: ItemId): LostReport?
}
