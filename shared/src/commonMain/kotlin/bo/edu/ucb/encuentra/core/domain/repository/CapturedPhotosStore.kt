package bo.edu.ucb.encuentra.core.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fotos del objeto encontrado que se está registrando (H01 → H03). Paso 1 (Captura) las agrega y el formulario
 * (Detalles y Ubicación) las guarda con el objeto. Vive en memoria mientras dura el registro: `NavRoute.FoundForm`
 * no lleva parámetros (NavRoute.kt congelado) y así volver atrás desde el formulario no pierde las fotos.
 */
class CapturedPhotosStore {
    private val state = MutableStateFlow<List<String>>(emptyList())
    val photos: StateFlow<List<String>> = state.asStateFlow()

    fun add(path: String) {
        if (path !in state.value) state.value = state.value + path
    }

    fun remove(path: String) {
        state.value = state.value - path
    }

    /** Registro terminado o descartado. */
    fun clear() {
        state.value = emptyList()
    }
}
