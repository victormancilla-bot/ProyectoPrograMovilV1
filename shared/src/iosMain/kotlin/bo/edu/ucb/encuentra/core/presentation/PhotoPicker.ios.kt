package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import bo.edu.ucb.encuentra.core.util.PhotoFolder

/** Pendiente en iOS (PHPickerViewController): no se puede compilar ni probar desde Windows. El botón se oculta. */
@Composable
actual fun rememberPhotoPicker(folder: PhotoFolder, onPicked: (String) -> Unit): PhotoPickerLauncher =
    remember { PhotoPickerLauncher(isAvailable = false) {} }
