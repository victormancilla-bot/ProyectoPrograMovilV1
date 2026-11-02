package bo.edu.ucb.encuentra.feature.matching.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/** "Esto es mío — verificar" (M05): pide a V02 que cree el reclamo. Devuelve el número del reclamo. */
class ClaimMatchUseCase(
    private val requester: ClaimRequester,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(matchId: ItemId): Result<Int> = runCatching {
        val user = getCurrentUser() ?: throw IllegalStateException("No hay sesión iniciada")
        requester.requestClaim(matchId, user.id)
    }
}
