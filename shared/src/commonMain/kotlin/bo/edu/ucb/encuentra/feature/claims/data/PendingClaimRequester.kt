package bo.edu.ucb.encuentra.feature.claims.data

import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.core.domain.repository.ClaimsNotAvailableException
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/** Placeholder hasta V02: el botón "Esto es mío" avisa que la verificación aún no está disponible. */
class PendingClaimRequester : ClaimRequester {
    override suspend fun requestClaim(matchId: ItemId, claimantId: String): Int = throw ClaimsNotAvailableException()
}
