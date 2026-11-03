package bo.edu.ucb.encuentra.feature.alerts.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Alertas del usuario con sesión (sin sesión, ninguna). */
class ObserveAlertsUseCase(
    private val repository: AlertsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<AppNotification>> =
        getCurrentUser()?.let { repository.observe(it.id) } ?: flowOf(emptyList())
}

/** Número del badge de la pestaña Alertas. Recibe el id porque la navegación lo vuelve a pedir al cambiar de usuario. */
class ObserveUnreadAlertsCountUseCase(private val repository: AlertsRepository) {
    operator fun invoke(userId: String?): Flow<Int> =
        userId?.let(repository::observeUnreadCount) ?: flowOf(0)
}

/** Marca una alerta como leída, solo si es del usuario con sesión y todavía no lo estaba. */
class MarkAlertReadUseCase(
    private val repository: AlertsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(alert: AppNotification) {
        if (alert.read || alert.userId != getCurrentUser()?.id) return
        repository.markRead(alert.id.value)
    }
}

class MarkAllAlertsReadUseCase(
    private val repository: AlertsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke() {
        getCurrentUser()?.let { repository.markAllRead(it.id) }
    }
}

/** "Limpiar": borra las leídas. Las no leídas se quedan para que no se pierdan sin verlas. */
class ClearReadAlertsUseCase(
    private val repository: AlertsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke() {
        getCurrentUser()?.let { repository.clearRead(it.id) }
    }
}
