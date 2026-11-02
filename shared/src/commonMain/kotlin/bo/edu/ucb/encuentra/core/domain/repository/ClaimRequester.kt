package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.vo.ItemId

/**
 * Hook de reclamos (V02): "Esto es mío — verificar" en el Detalle de match (M05) crea el reclamo.
 * V02 lo implementa (ClaimEntity, bloqueo del doble reclamo, objeto → RECLAMADO con H05).
 * Devuelve el número visible del reclamo ("Reclamo #1024").
 */
interface ClaimRequester {
    suspend fun requestClaim(matchId: ItemId, claimantId: String): Int
}

/** El flujo de reclamos todavía no está disponible (V02 pendiente). */
class ClaimsNotAvailableException : IllegalStateException("La verificación de reclamos todavía no está disponible.")
