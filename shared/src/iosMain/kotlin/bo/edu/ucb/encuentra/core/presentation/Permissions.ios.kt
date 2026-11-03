package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Pendiente en iOS (AVCaptureDevice, CLLocationManager y UNUserNotificationCenter): no se puede compilar desde
 * Windows. Por ahora se informa "no concedido" y la app usa las alternativas (galería, lista de zonas, bandeja).
 */
@Composable
actual fun rememberPermissionRequester(permission: AppPermission, onResult: (granted: Boolean) -> Unit): PermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(permission) {
        PermissionRequester(
            isGranted = { false },
            isPermanentlyDenied = { false },
            request = { currentOnResult(false) },
            openSettings = {}
        )
    }
}
