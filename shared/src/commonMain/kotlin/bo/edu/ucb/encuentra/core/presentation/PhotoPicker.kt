package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.util.PhotoFolder

/**
 * Abre el selector de fotos de la galería. [isAvailable] es false donde todavía no está implementado (iOS):
 * la pantalla oculta el botón en ese caso.
 */
class PhotoPickerLauncher(val isAvailable: Boolean, private val onLaunch: () -> Unit) {
    fun launch() = onLaunch()
}

/**
 * [onPicked] recibe la ruta (`file://...`) de una copia comprimida de la foto (H01) guardada en [folder] dentro del
 * almacenamiento privado de la app.
 */
@Composable
expect fun rememberPhotoPicker(folder: PhotoFolder = PhotoFolder.REPORTS, onPicked: (String) -> Unit): PhotoPickerLauncher
