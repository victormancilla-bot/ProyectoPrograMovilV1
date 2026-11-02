package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/**
 * Hook de reclamos para el Detalle de match (M05), implementado en `feature/claims` (V02 / V01):
 * el estudiante reclama y después responde las preguntas de control sin que `matching` dependa de `claims`.
 */
interface ClaimRequester {
    /** "Esto es mío — verificar": crea el reclamo y devuelve su número visible ("Reclamo #1024"). */
    suspend fun requestClaim(matchId: ItemId, claimantId: String): Int

    /**
     * Respuestas a las preguntas de control (V01). Devuelve el reclamo actualizado: EN_VERIFICACION si coincidieron,
     * PENDIENTE con un intento fallido más si no, o RECHAZADO si se acabaron los intentos.
     */
    suspend fun answerQuestions(claimId: ItemId, answers: List<String>): Claim
}

/** Error de un reclamo con un mensaje que se puede mostrar tal cual al usuario. */
open class ClaimRequestException(message: String) : IllegalStateException(message)

/** Ya existe un reclamo de esta persona para este objeto (V02 bloquea el doble reclamo). */
class DuplicateClaimException(val number: Int) :
    ClaimRequestException("Ya reclamaste este objeto (Reclamo #$number).")
