package bo.edu.ucb.encuentra.feature.dashboard.domain.repository

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportData
import kotlinx.coroutines.flow.Flow

/** D03 · Datos para exportar a CSV, en vivo. */
interface ExportRepository {
    fun observeData(): Flow<ExportData>
}
