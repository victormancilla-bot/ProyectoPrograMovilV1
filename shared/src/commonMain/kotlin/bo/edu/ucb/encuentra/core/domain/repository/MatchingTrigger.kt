package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport

/**
 * Hook del matching (M01): se llama al guardar un reporte de perdido (L04) y al registrar un hallazgo (H03).
 * La implementación compara contra lo que hay en Room y guarda los Match nuevos.
 */
interface MatchingTrigger {
    suspend fun onReportCreated(report: LostReport)

    suspend fun onFoundItemRegistered(item: FoundItem)
}
