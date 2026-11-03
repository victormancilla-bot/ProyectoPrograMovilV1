package bo.edu.ucb.encuentra.core.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** El permiso de Android que corresponde; null = no hace falta pedirlo (notificaciones antes de Android 13). */
private fun AppPermission.manifestPermission(): String? = when (this) {
    AppPermission.NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
    AppPermission.CAMERA -> Manifest.permission.CAMERA
    AppPermission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun rememberPermissionRequester(permission: AppPermission, onResult: (granted: Boolean) -> Unit): PermissionRequester {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val manifestPermission = permission.manifestPermission()
    // Android no dice si "nunca lo pidió" o "lo negó para siempre": se recuerda si ya se pidió en esta pantalla.
    var askedOnce by remember(permission) { mutableStateOf(false) }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        askedOnce = true
        currentOnResult(granted)
    }
    // Ubicación: se piden precisa y aproximada juntas (el usuario puede elegir solo la aproximada).
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        askedOnce = true
        currentOnResult(result.values.any { it })
    }

    fun granted(): Boolean = manifestPermission == null ||
        ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED ||
        (permission == AppPermission.LOCATION &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

    return remember(permission, single, multiple) {
        PermissionRequester(
            isGranted = ::granted,
            isPermanentlyDenied = {
                val activity = context.findActivity()
                manifestPermission != null && !granted() && askedOnce && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission)
            },
            request = {
                when {
                    manifestPermission == null || granted() -> currentOnResult(true)
                    permission == AppPermission.LOCATION -> multiple.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                    else -> single.launch(manifestPermission)
                }
            },
            openSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }
}
