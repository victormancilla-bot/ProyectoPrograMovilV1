package bo.edu.ucb.encuentra.feature.myreports.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportMatchSummary
import kotlinx.coroutines.flow.Flow

interface MyReportsRepository {
    /** Reportes del usuario, en vivo desde Room. */
    fun observeByOwner(ownerId: String): Flow<List<LostReport>>

    fun observeReport(reportId: ItemId): Flow<LostReport?>

    /** Coincidencias no descartadas del reporte con los datos del objeto encontrado. */
    fun observeMatches(reportId: ItemId): Flow<List<ReportMatchSummary>>

    suspend fun getReport(reportId: ItemId): LostReport?

    suspend fun updateStatus(reportId: ItemId, status: ReportStatus)
}
