package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.LostReport

/**
 * Hook del matching (M01): se llama apenas se guarda un reporte de objeto perdido (L04).
 * M01 lo implementa comparando el reporte con los objetos DISPONIBLES y guardando los Match.
 */
interface MatchingTrigger {
    suspend fun onReportCreated(report: LostReport)
}
