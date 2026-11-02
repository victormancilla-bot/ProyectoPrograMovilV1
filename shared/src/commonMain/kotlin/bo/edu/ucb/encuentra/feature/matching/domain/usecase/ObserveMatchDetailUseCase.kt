package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.matching.domain.engine.MatchEngine
import bo.edu.ucb.encuentra.feature.matching.domain.model.ComparisonVerdict
import bo.edu.ucb.encuentra.feature.matching.domain.model.ExistingClaim
import bo.edu.ucb.encuentra.feature.matching.domain.model.FoundItemPreview
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchComparison
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchDetail
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchPair
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Detalle de match (M05). Devuelve null si la coincidencia no existe, fue descartada ("No es mío") o el reporte
 * no es del usuario actual: un deep link con el id de otro no muestra nada.
 * La comparación usa las mismas similitudes que el score (M02): ✔ solo si el dato coincide del todo.
 */
class ObserveMatchDetailUseCase(
    private val repository: MatchRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(matchId: ItemId): Flow<MatchDetail?> {
        val user = getCurrentUser() ?: return flowOf(null)
        return repository.observeMatch(matchId).map { pair ->
            // Una descartada se sigue mostrando si el dueño la reclamó: así ve en qué quedó su reclamo (p. ej. bloqueado).
            pair?.takeIf { it.report.ownerId == user.id && (it.match.status != MatchStatus.DESCARTADO || it.ownClaim != null) }
                ?.toDetail()
        }
    }

    private fun MatchPair.toDetail(): MatchDetail = MatchDetail(
        matchId = match.id,
        score = match.score,
        status = match.status,
        report = report,
        item = FoundItemPreview(
            title = item.title,
            subcategory = item.subcategory,
            color = item.color,
            brand = item.brand,
            zone = item.foundZone,
            foundAt = item.foundAt,
            photoUrl = item.photoUrls.firstOrNull()
        ),
        comparison = MatchComparison(
            type = verdict(if (report.subcategory == item.subcategory) 1.0 else 0.0),
            brand = verdict(MatchEngine.brandSimilarity(report.brand, item.brand)),
            color = verdict(MatchEngine.colorSimilarity(report.color, item.color)),
            zone = verdict(MatchEngine.zoneSimilarity(report.zone, item.foundZone)),
            // Encontrado antes de perderlo no puede ser: el score no lo mira (lo descarta el filtro duro), la tabla sí.
            date = if (item.foundAt < report.lostFrom) ComparisonVerdict.POR_VERIFICAR else verdict(MatchEngine.dateSimilarity(report, item))
        ),
        existingClaim = ownClaim?.let { claim ->
            ExistingClaim(
                id = claim.id,
                number = claim.number,
                status = claim.status,
                failedAttempts = claim.failedAttempts,
                // Solo mientras puede responder; las respuestas esperadas nunca salen de claims.
                questions = if (claim.status == ClaimStatus.PENDIENTE) item.controlQuestions else emptyList()
            )
        }
    )

    private fun verdict(similarity: Double) =
        if (similarity >= 1.0) ComparisonVerdict.COINCIDE else ComparisonVerdict.POR_VERIFICAR
}
