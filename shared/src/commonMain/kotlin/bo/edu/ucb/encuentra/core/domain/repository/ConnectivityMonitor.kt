package bo.edu.ucb.encuentra.core.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * F11 · ¿Hay internet? Cada plataforma lo implementa en su `platformModule()` (Android: ConnectivityManager).
 * La app es offline-first (Room): sin conexión se sigue usando, solo se avisa con el banner.
 */
interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>

    /** Vuelve a consultar el estado (botón "Reintentar" del banner). */
    fun recheck()
}

/** iOS (pendiente NWPathMonitor) y tests: siempre conectado. */
class AlwaysOnline : ConnectivityMonitor {
    private val state = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()
    override fun recheck() = Unit
}
