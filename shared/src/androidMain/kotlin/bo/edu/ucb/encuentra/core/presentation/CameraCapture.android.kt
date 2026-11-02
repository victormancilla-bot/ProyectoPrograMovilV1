package bo.edu.ucb.encuentra.core.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import bo.edu.ucb.encuentra.core.util.PhotoFolder
import kotlinx.coroutines.launch
import java.io.File

/**
 * Cámara del sistema con `TakePicture` (la app no necesita su propia vista de cámara). La foto se escribe en un
 * archivo temporal compartido con [FileProvider], después se comprime a la carpeta de la app y el temporal se borra.
 *
 * Permiso CAMERA (declarado en el manifest, así que hay que pedirlo): si ya está, abre la cámara; si el usuario lo
 * negó antes, avisa [CameraPermissionResult.NEEDS_RATIONALE] para que la pantalla explique y llame a
 * [CameraLauncher.requestPermission]; si lo niega con "No volver a preguntar", [CameraPermissionResult.PERMANENTLY_DENIED].
 */
@Composable
actual fun rememberCamera(
    folder: PhotoFolder,
    onCaptured: (String) -> Unit,
    onPermission: (CameraPermissionResult) -> Unit
): CameraLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnPermission by rememberUpdatedState(onPermission)
    var pending by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pending
        pending = null
        if (saved && file != null) {
            scope.launch {
                compressToAppStorage(context, Uri.fromFile(file), folder)?.let { currentOnCaptured(it) }
                file.delete()
            }
        } else {
            file?.delete()
        }
    }

    fun openCamera() {
        val file = newTempPhoto(context)
        pending = file
        takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> openCamera()
            // Tras negar, si el sistema ya no deja mostrar el diálogo, quedó negado "para siempre".
            context.findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == false ->
                currentOnPermission(CameraPermissionResult.PERMANENTLY_DENIED)
            else -> currentOnPermission(CameraPermissionResult.DENIED)
        }
    }

    return remember(takePicture, requestPermission) {
        CameraLauncher(
            isAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            onLaunch = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                val activity = context.findActivity()
                when {
                    granted -> openCamera()
                    activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ->
                        currentOnPermission(CameraPermissionResult.NEEDS_RATIONALE)
                    else -> requestPermission.launch(Manifest.permission.CAMERA)
                }
            },
            onRequestPermission = { requestPermission.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }
}

/** Temporal en `cache/camera` (lo comparte el FileProvider, ver res/xml/file_paths.xml de androidApp). */
private fun newTempPhoto(context: Context): File {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    return File.createTempFile("captura_", ".jpg", dir)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
