package bo.edu.ucb.encuentra.feature.matching.domain.model

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match

/** Un objeto encontrado candidato para un reporte, con su score (M04: nunca un "sí/no"). */
data class MatchCandidate(val match: Match, val item: FoundItem)

/** Un reporte del usuario con sus candidatos, de mayor a menor score. */
data class ReportMatches(val report: LostReport, val candidates: List<MatchCandidate>)
