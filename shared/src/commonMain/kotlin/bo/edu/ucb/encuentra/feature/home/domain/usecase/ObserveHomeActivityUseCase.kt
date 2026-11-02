package bo.edu.ucb.encuentra.feature.home.domain.usecase

import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.feature.home.domain.model.HomeActivity
import bo.edu.ucb.encuentra.feature.home.domain.repository.HomeActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Actividad del estudiante con sesión iniciada. Sin sesión (no debería pasar por el guard) devuelve ceros. */
class ObserveHomeActivityUseCase(
    private val repository: HomeActivityRepository,
    private val session: SessionRepository
) {
    operator fun invoke(): Flow<HomeActivity> {
        val user = session.getCurrentUser() ?: return flowOf(HomeActivity.EMPTY)
        return repository.observe(user.id)
    }
}
