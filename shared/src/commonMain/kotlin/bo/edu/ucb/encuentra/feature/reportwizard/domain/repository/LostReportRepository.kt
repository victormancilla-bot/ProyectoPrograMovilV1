package bo.edu.ucb.encuentra.feature.reportwizard.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.LostReport

interface LostReportRepository {
    /** Guarda el reporte en Room. L06 lo sube también a Firestore. */
    suspend fun save(report: LostReport)
}
