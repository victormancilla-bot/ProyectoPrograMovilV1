package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository

class GetCurrentUserUseCase(private val session: SessionRepository) {
    operator fun invoke(): UserModel? = session.getCurrentUser()
}
