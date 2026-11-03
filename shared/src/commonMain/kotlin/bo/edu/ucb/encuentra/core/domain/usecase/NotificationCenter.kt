package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier
import bo.edu.ucb.encuentra.core.domain.repository.NotificationRepository

/**
 * Centro de notificaciones internas (N01). Todas las features publican aquí (matching: coincidencias nuevas;
 * reclamos: aprobado, rechazado, entrega programada, entregado):
 * 1. se guardan en Room (las ve la Bandeja de Alertas, N04, y el Home cuenta las de coincidencias);
 * 2. se muestran en el sistema SOLO si el destinatario es quien tiene la sesión abierta. En la demo todos usan el
 *    mismo dispositivo: si el staff aprueba el reclamo de Ana, el aviso no debe aparecerle al staff. A Ana se le
 *    muestra cuando entra ([deliverPending]). Entre dispositivos lo resuelve el push de N02.
 */
class NotificationCenter(
    private val repository: NotificationRepository,
    private val notifier: LocalNotifier,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend fun publish(notifications: List<AppNotification>) {
        if (notifications.isEmpty()) return
        repository.save(notifications)
        val user = getCurrentUser() ?: return
        val mine = notifications.filter { it.userId == user.id }
        mine.forEach(notifier::show)
        mine.maxOfOrNull { it.createdAt.toEpochMilliseconds() }?.let { latest ->
            if (latest > repository.shownUntil(user.id)) repository.setShownUntil(user.id, latest)
        }
    }

    suspend fun publish(notification: AppNotification) = publish(listOf(notification))

    /**
     * Al entrar a la app (o después de dar el permiso): muestra las no leídas del usuario que todavía no se
     * mostraron en este dispositivo, como máximo [MAX_PENDING] (las más recientes) para no llenar la barra.
     */
    suspend fun deliverPending(): Int {
        val user = getCurrentUser() ?: return 0
        val pending = repository.unreadSince(user.id, repository.shownUntil(user.id))
        if (pending.isEmpty()) return 0
        pending.takeLast(MAX_PENDING).forEach(notifier::show)
        repository.setShownUntil(user.id, pending.maxOf { it.createdAt.toEpochMilliseconds() })
        return pending.size.coerceAtMost(MAX_PENDING)
    }

    companion object {
        const val MAX_PENDING = 5
    }
}
