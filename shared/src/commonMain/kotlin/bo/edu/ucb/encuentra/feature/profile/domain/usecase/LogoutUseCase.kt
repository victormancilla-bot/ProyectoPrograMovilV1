package bo.edu.ucb.encuentra.feature.profile.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.PushTokenRegistry
import bo.edu.ucb.encuentra.core.domain.repository.RemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

/** Cerrar sesión (S07): primero se borra el token push (N02) para que este teléfono deje de recibir sus avisos. */
class LogoutUseCase(
    private val session: SessionRepository,
    private val pushTokens: PushTokenRegistry,
    /** S02: también se cierra la sesión de Firebase (cuentas Google). */
    private val remoteAuth: RemoteAuth
) {
    suspend operator fun invoke() {
        runCatching { pushTokens.unregister() }
        runCatching { remoteAuth.signOut() }
        session.clearSession()
    }
}
