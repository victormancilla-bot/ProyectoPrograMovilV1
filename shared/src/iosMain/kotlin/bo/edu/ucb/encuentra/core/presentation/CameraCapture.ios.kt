package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import bo.edu.ucb.encuentra.core.util.PhotoFolder

/** Pendiente en iOS (UIImagePickerController + NSCameraUsageDescription): no se puede compilar desde Windows. */
@Composable
actual fun rememberCamera(
    folder: PhotoFolder,
    onCaptured: (String) -> Unit,
    onPermission: (CameraPermissionResult) -> Unit
): CameraLauncher = remember { CameraLauncher(isAvailable = false, onLaunch = {}, onRequestPermission = {}, onOpenSettings = {}) }
