package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pide el permiso de notificaciones (Android 13+: POST_NOTIFICATIONS). Devuelve la función que lo pide;
 * [onResult] recibe si se puede notificar. Donde no hace falta pedirlo, responde enseguida.
 */
@Composable
expect fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit

/**
 * N01: cuando entra un usuario (o cambia), pide el permiso de notificaciones y muestra sus avisos pendientes
 * (p. ej. a Ana le llega "Reclamo #1024 aprobado" aunque el staff lo haya aprobado en este mismo teléfono).
 */
@Composable
fun LocalNotificationsEffect(userId: String?, center: NotificationCenter = koinInject()) {
    val scope = rememberCoroutineScope()
    // Aunque niegue el permiso, se marca lo pendiente como entregado: sigue disponible en la bandeja (N04).
    val request = rememberNotificationPermission { scope.launch { center.deliverPending() } }
    LaunchedEffect(userId) {
        if (userId != null) request()
    }
}
