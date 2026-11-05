package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth

import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

/**
 * Al abrir la app: recupera la sesión guardada (si la hay) para entrar directo al home del rol.
 * S02: si la cuenta vino de Google pero la sesión de Firebase ya no existe en el teléfono (expiró, se revocó o
 * se borró), se cierra también la sesión local y se vuelve al Login. Las cuentas de prueba (`u-...`) no dependen
 * de Firebase.
 */
class RestoreSessionUseCase(
    private val session: SessionRepository,
    private val remoteAuth: RemoteAuth
) {
    suspend operator fun invoke() {
        session.restore()
        val user = session.getCurrentUser() ?: return
        if (!user.id.startsWith(DEMO_ID_PREFIX) && remoteAuth.currentUid() != user.id) session.clearSession()
    }

    companion object {
        /** Ids de las cuentas de prueba del seed (Room). */
        const val DEMO_ID_PREFIX = "u-"
    }
}
