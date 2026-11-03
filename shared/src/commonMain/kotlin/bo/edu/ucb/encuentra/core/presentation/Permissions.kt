package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.theme.UcbEncuentraTheme

/**
 * F11 · Permisos que usa la app, con su justificación (clase "Permisos en Android": pedir solo lo necesario,
 * explicar para qué y ofrecer una alternativa si se niega).
 */
enum class AppPermission(val title: String, val why: String, val fallback: String) {
    NOTIFICATIONS(
        "Avisos",
        "Te avisamos cuando aparece un objeto parecido al que perdiste, cuando cambia tu reclamo y, si eres staff, " +
            "cuando un objeto está por vencer. No enviamos publicidad.",
        "Sin este permiso, los avisos igual quedan en tu bandeja de Alertas."
    ),
    CAMERA(
        "Cámara",
        "Se usa solo para fotografiar los objetos encontrados, así su dueño puede reconocerlo. Las fotos se guardan en la app.",
        "Sin este permiso puedes subir las fotos desde la galería."
    ),
    LOCATION(
        "Ubicación",
        "Sirve para marcar en el mapa dónde perdiste o encontraste algo. Solo se usa mientras eliges el lugar.",
        "Sin este permiso eliges la zona del campus de una lista."
    );

    val icon: ImageVector
        get() = when (this) {
            NOTIFICATIONS -> Icons.Outlined.NotificationsActive
            CAMERA -> Icons.Outlined.CameraAlt
            LOCATION -> Icons.Outlined.LocationOn
        }
}

/**
 * Estado y acciones de un permiso. [isGranted] se lee al momento (puede cambiar en Ajustes);
 * [isPermanentlyDenied] = ya lo negó y el sistema no vuelve a preguntar: solo queda abrir Ajustes.
 */
class PermissionRequester(
    val isGranted: () -> Boolean,
    val isPermanentlyDenied: () -> Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit
)

/** Expect/actual: Android usa los permisos de ejecución; iOS queda pendiente (no se compila desde Windows). */
@Composable
expect fun rememberPermissionRequester(permission: AppPermission, onResult: (granted: Boolean) -> Unit = {}): PermissionRequester

/**
 * Justificación antes de pedir un permiso (o antes de mandar a Ajustes si ya se negó para siempre). Es el mismo
 * diálogo para cámara, ubicación y notificaciones.
 */
@Composable
fun PermissionRationaleDialog(
    permission: AppPermission,
    permanentlyDenied: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = UcbEncuentraTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(permission.icon, contentDescription = null, tint = colors.primary) },
        title = { AppTitle(if (permanentlyDenied) "${permission.title} desactivado" else "Permiso de ${permission.title.lowercase()}") },
        text = {
            AppBody(
                listOfNotNull(
                    permission.why,
                    if (permanentlyDenied) "Actívalo en Ajustes > Permisos de UCB Encuentra." else null,
                    permission.fallback
                ).joinToString("\n\n")
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                AppLabel(if (permanentlyDenied) "Abrir ajustes" else "Permitir", color = colors.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { AppLabel("Ahora no", color = colors.primary) } }
    )
}
