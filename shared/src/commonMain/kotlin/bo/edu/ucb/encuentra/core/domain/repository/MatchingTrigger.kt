package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport

/**
 * Hook del matching (M01-M02): se llama al guardar o editar un reporte de perdido (L04 / L05) y al registrar
 * un hallazgo (H03). La implementación re-calcula el ranking de los reportes afectados.
 */
interface MatchingTrigger {
    suspend fun onReportCreated(report: LostReport)

    /** Re-matching al editar el reporte (L05) o al marcar "No es mío" (para completar el Top-N). */
    suspend fun onReportUpdated(report: LostReport)

    suspend fun onFoundItemRegistered(item: FoundItem)
}
