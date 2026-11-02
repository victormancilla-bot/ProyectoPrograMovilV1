package bo.edu.ucb.encuentra.feature.claims.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/**
 * Reclamos (V02) y lo que cambia con ellos: la coincidencia y el reporte del dueño.
 * El estado del objeto se cambia con [bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository] (H05).
 * Los use cases llaman a todo dentro de una sola transacción.
 */
interface ClaimRepository {
    suspend fun getById(id: ItemId): Claim?

    /** Todos los reclamos de un objeto, en cualquier estado. */
    suspend fun getByItem(itemId: ItemId): List<Claim>

    suspend fun getByClaimant(claimantId: String): List<Claim>

    /** Siguiente número visible ("Reclamo #1026"). */
    suspend fun nextNumber(): Int

    /** Falla si la persona ya reclamó ese objeto (índice único en Room). */
    suspend fun insert(claim: Claim)

    suspend fun update(claim: Claim)

    suspend fun getMatch(id: ItemId): Match?

    suspend fun updateMatchStatus(id: ItemId, status: MatchStatus)

    suspend fun getReport(id: ItemId): LostReport?

    suspend fun updateReportStatus(id: ItemId, status: ReportStatus)
}
