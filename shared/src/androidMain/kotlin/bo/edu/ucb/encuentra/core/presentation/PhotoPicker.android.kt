package bo.edu.ucb.encuentra.core.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import bo.edu.ucb.encuentra.core.util.PhotoFolder
import kotlinx.coroutines.launch

/**
 * Photo Picker del sistema: no necesita permisos de almacenamiento (el usuario elige la foto y la app solo
 * recibe esa). En Android < 13 lo provee Google Play services. La foto se comprime y se copia a la carpeta de la
 * app ([compressToAppStorage]).
 */
@Composable
actual fun rememberPhotoPicker(folder: PhotoFolder, onPicked: (String) -> Unit): PhotoPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { compressToAppStorage(context, uri, folder)?.let { currentOnPicked(it) } }
        }
    }
    return remember(launcher) {
        PhotoPickerLauncher(isAvailable = true) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
