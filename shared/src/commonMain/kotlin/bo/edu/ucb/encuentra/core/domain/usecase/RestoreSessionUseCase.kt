package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

/** Al abrir la app: recupera la sesión guardada (si la hay) para entrar directo al home del rol. */
class RestoreSessionUseCase(private val session: SessionRepository) {
    suspend operator fun invoke() = session.restore()
}
