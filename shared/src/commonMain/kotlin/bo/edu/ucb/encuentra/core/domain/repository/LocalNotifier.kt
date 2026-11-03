package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppNotification

/**
 * Notificación del sistema en el dispositivo (N01). Cada plataforma la implementa y la registra en su
 * `platformModule()` (expect/actual): Android con canales y `NotificationCompat`; iOS pendiente.
 * Si el usuario no dio el permiso de notificaciones, simplemente no se muestra (queda en la bandeja de la app).
 */
interface LocalNotifier {
    fun show(notification: AppNotification)
}

/**
 * Notificaciones internas guardadas en Room (tabla `notifications`) y hasta qué momento ya se mostraron en el
 * sistema a cada usuario (así al volver a entrar no se repiten).
 */
interface NotificationRepository {
    suspend fun save(notifications: List<AppNotification>)

    /** No leídas de [userId] creadas después de [sinceMillis], de la más antigua a la más reciente. */
    suspend fun unreadSince(userId: String, sinceMillis: Long): List<AppNotification>

    suspend fun shownUntil(userId: String): Long

    suspend fun setShownUntil(userId: String, millis: Long)
}
