package bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel

import bo.edu.ucb.encuentra.core.presentation.CameraPermissionResult

/** Paso 1 del registro (H01, mockup "Registrar – Captura de Foto"): evidencia fotográfica. */
data class FoundCaptureState(
    val photos: List<String> = emptyList(),
    /** Aviso del permiso CAMERA: justificación, negado o negado para siempre. */
    val permission: CameraPermissionResult? = null
) {
    val canAddPhoto: Boolean get() = photos.size < MAX_PHOTOS
    val canContinue: Boolean get() = photos.isNotEmpty()

    companion object {
        /** Mínimo 1 foto; varias ángulos ayudan a identificarlo, pero más de 6 no aporta y pesa. */
        const val MAX_PHOTOS = 6
    }
}

sealed class FoundCaptureEvent {
    /** Foto ya comprimida, de la cámara o de la galería. */
    data class OnPhotoAdded(val path: String) : FoundCaptureEvent()
    data class OnRemovePhoto(val path: String) : FoundCaptureEvent()
    data class OnPermissionResult(val result: CameraPermissionResult) : FoundCaptureEvent()
    data object OnDismissPermission : FoundCaptureEvent()
    data object OnContinue : FoundCaptureEvent()

    /** Salir del registro: se descartan las fotos. */
    data object OnCancel : FoundCaptureEvent()
}

sealed class FoundCaptureEffect {
    data object NavigateToForm : FoundCaptureEffect()
    data object NavigateBack : FoundCaptureEffect()
}
