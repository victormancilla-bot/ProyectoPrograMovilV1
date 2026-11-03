package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import bo.edu.ucb.encuentra.core.domain.repository.PreferencesRepository
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * N01 + F11: cuando entra un usuario muestra sus avisos pendientes. Si el teléfono todavía no dio el permiso de
 * notificaciones, primero explica para qué son (una sola vez por teléfono) y recién entonces lo pide. Si dice
 * "Ahora no", no se insiste: puede activarlo luego en Perfil > Permisos y los avisos siguen en la bandeja.
 */
@Composable
fun LocalNotificationsEffect(
    userId: String?,
    center: NotificationCenter = koinInject(),
    preferences: PreferencesRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    var showRationale by remember { mutableStateOf(false) }
    val requester = rememberPermissionRequester(AppPermission.NOTIFICATIONS) { scope.launch { center.deliverPending() } }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        when {
            requester.isGranted() -> center.deliverPending()
            !preferences.observeDevice().first().notificationsRationaleShown -> showRationale = true
            // Ya se explicó y no lo quiso: los avisos quedan en la bandeja (se marcan como entregados).
            else -> center.deliverPending()
        }
    }

    if (showRationale) {
        val close: (Boolean) -> Unit = { ask ->
            showRationale = false
            scope.launch {
                preferences.markNotificationsRationaleShown()
                if (ask) requester.request() else center.deliverPending()
            }
        }
        PermissionRationaleDialog(
            permission = AppPermission.NOTIFICATIONS,
            permanentlyDenied = false,
            onContinue = { close(true) },
            onDismiss = { close(false) }
        )
    }
}
