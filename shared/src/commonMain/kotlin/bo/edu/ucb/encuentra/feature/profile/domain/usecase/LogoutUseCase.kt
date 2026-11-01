package bo.edu.ucb.encuentra.feature.profile.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

class LogoutUseCase(private val session: SessionRepository) {
    operator fun invoke() = session.clearSession()
}
