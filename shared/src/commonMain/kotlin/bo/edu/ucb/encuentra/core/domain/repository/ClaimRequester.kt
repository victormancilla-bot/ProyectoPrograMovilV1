package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/**
 * Hook de reclamos: "Esto es mío — verificar" en el Detalle de match (M05) crea el reclamo.
 * Lo implementa V02 (`CreateClaimUseCase`). Devuelve el número visible del reclamo ("Reclamo #1024").
 */
interface ClaimRequester {
    suspend fun requestClaim(matchId: ItemId, claimantId: String): Int
}

/** Error de un reclamo con un mensaje que se puede mostrar tal cual al usuario. */
open class ClaimRequestException(message: String) : IllegalStateException(message)

/** Ya existe un reclamo de esta persona para este objeto (V02 bloquea el doble reclamo). */
class DuplicateClaimException(val number: Int) :
    ClaimRequestException("Ya reclamaste este objeto (Reclamo #$number).")
