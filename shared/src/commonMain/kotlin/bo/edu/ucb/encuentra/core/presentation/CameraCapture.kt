package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.util.PhotoFolder

/** Qué pasó con el permiso CAMERA al intentar tomar una foto (H01). */
enum class CameraPermissionResult {
    /** El usuario ya lo negó una vez: hay que explicar para qué se usa antes de pedirlo de nuevo. */
    NEEDS_RATIONALE,

    /** Lo negó ahora: se puede volver a intentar o usar la galería. */
    DENIED,

    /** Lo negó con "No volver a preguntar": solo se puede activar desde Ajustes. */
    PERMANENTLY_DENIED
}

/**
 * Cámara nativa (H01), expect/actual propio en vez de una librería KMP: sigue el mismo patrón que [PhotoPickerLauncher]
 * y no agrega dependencias. [isAvailable] es false donde todavía no está implementada (iOS) y la pantalla oculta el botón.
 */
class CameraLauncher(
    val isAvailable: Boolean,
    private val onLaunch: () -> Unit,
    private val onRequestPermission: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    /** Abre la cámara; si falta el permiso lo pide (o avisa con [CameraPermissionResult]). */
    fun launch() = onLaunch()

    /** Pide el permiso de nuevo, después de mostrar la justificación. */
    fun requestPermission() = onRequestPermission()

    /** Abre los ajustes de la app (permiso negado para siempre). */
    fun openSettings() = onOpenSettings()
}

/**
 * [onCaptured] recibe la ruta (`file://...`) de la foto ya comprimida y guardada en [folder].
 * [onPermission] avisa cuando el permiso CAMERA no está concedido.
 */
@Composable
expect fun rememberCamera(
    folder: PhotoFolder,
    onCaptured: (String) -> Unit,
    onPermission: (CameraPermissionResult) -> Unit
): CameraLauncher
