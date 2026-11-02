package bo.edu.ucb.encuentra.core.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Photo Picker del sistema: no necesita permisos de almacenamiento (el usuario elige la foto y la app solo
 * recibe esa). En Android < 13 lo provee Google Play services. La foto se copia a `filesDir/report_photos`
 * porque el permiso de lectura del URI del picker se pierde al cerrar la app.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { copyToAppStorage(context, uri)?.let { currentOnPicked(it) } }
        }
    }
    return remember(launcher) {
        PhotoPickerLauncher(isAvailable = true) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun copyToAppStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        val file = File(dir, "${Uuid.random()}.jpg")
        val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
        input.use { source -> file.outputStream().use { source.copyTo(it) } }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

private const val PHOTOS_DIR = "report_photos"
