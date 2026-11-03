package bo.edu.ucb.encuentra.feature.profile.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.PushTokenRegistry
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

/** Cerrar sesión (S07): primero se borra el token push (N02) para que este teléfono deje de recibir sus avisos. */
class LogoutUseCase(
    private val session: SessionRepository,
    private val pushTokens: PushTokenRegistry
) {
    suspend operator fun invoke() {
        runCatching { pushTokens.unregister() }
        session.clearSession()
    }
}
