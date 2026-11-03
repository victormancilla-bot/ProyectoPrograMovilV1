package bo.edu.ucb.encuentra.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import bo.edu.ucb.encuentra.core.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * F11 · Conexión en vivo con `registerDefaultNetworkCallback` (permiso ACCESS_NETWORK_STATE). Se considera
 * conectado si la red por defecto tiene internet.
 */
class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {

    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    init {
        runCatching {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = recheck()
                override fun onLost(network: Network) {
                    state.value = false
                }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        }
    }

    override fun recheck() {
        state.value = currentlyOnline()
    }

    private fun currentlyOnline(): Boolean {
        val capabilities = manager?.activeNetwork?.let { manager.getNetworkCapabilities(it) } ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
