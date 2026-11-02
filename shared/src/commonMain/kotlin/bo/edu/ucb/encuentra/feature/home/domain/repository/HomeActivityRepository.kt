package bo.edu.ucb.encuentra.feature.home.domain.repository

import bo.edu.ucb.encuentra.feature.home.domain.model.HomeActivity
import kotlinx.coroutines.flow.Flow

interface HomeActivityRepository {
    /** Se actualiza solo cuando cambian los reportes o las notificaciones del usuario (Room + Flow). */
    fun observe(userId: String): Flow<HomeActivity>
}
