package bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.ucb.encuentra.core.domain.repository.CapturedPhotosStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Captura de fotos (H01). Las fotos viven en [CapturedPhotosStore] para que el formulario (pasos 2 y 3) las reciba y
 * para no perderlas al volver atrás desde el formulario. Abrir la cámara y pedir el permiso es de la pantalla
 * (`rememberCamera`); aquí solo se refleja el resultado.
 */
class FoundCaptureViewModel(private val store: CapturedPhotosStore) : ViewModel() {

    private val _state = MutableStateFlow(FoundCaptureState(photos = store.photos.value))
    val state: StateFlow<FoundCaptureState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<FoundCaptureEffect>()
    val effects: SharedFlow<FoundCaptureEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch { store.photos.collect { _state.value = state.value.copy(photos = it) } }
    }

    fun handleEvent(event: FoundCaptureEvent) {
        when (event) {
            // El límite se mira en el borrador (el estado de la pantalla se actualiza con un poco de retraso).
            is FoundCaptureEvent.OnPhotoAdded -> if (store.photos.value.size < FoundCaptureState.MAX_PHOTOS) store.add(event.path)
            is FoundCaptureEvent.OnRemovePhoto -> store.remove(event.path)
            is FoundCaptureEvent.OnPermissionResult -> _state.value = state.value.copy(permission = event.result)
            FoundCaptureEvent.OnDismissPermission -> _state.value = state.value.copy(permission = null)
            FoundCaptureEvent.OnContinue -> if (state.value.canContinue) emit(FoundCaptureEffect.NavigateToForm)
            FoundCaptureEvent.OnCancel -> {
                store.clear()
                emit(FoundCaptureEffect.NavigateBack)
            }
        }
    }

    private fun emit(effect: FoundCaptureEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
