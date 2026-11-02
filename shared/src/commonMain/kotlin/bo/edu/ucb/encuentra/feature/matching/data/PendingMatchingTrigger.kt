package bo.edu.ucb.encuentra.feature.matching.data

import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger

/** Mientras M01 no exista, el envío del reporte llama a este hook y no se genera ningún match. M01 lo reemplaza. */
class PendingMatchingTrigger : MatchingTrigger {
    override suspend fun onReportCreated(report: LostReport) = Unit
}
