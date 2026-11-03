package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier

/**
 * Pendiente en iOS (UNUserNotificationCenter + permiso): no se puede compilar ni probar desde Windows.
 * Las notificaciones igual quedan guardadas en Room y se ven en la bandeja de la app.
 */
class IosLocalNotifier : LocalNotifier {
    override fun show(notification: AppNotification) = Unit
}
