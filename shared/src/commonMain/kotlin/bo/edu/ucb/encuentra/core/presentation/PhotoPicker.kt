package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable

/**
 * Abre el selector de fotos de la galería. [isAvailable] es false donde todavía no está implementado (iOS):
 * la pantalla oculta el botón en ese caso. La cámara llega con H01.
 */
class PhotoPickerLauncher(val isAvailable: Boolean, private val onLaunch: () -> Unit) {
    fun launch() = onLaunch()
}

/** [onPicked] recibe la ruta (`file://...`) de una copia de la foto guardada en el almacenamiento de la app. */
@Composable
expect fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPickerLauncher
