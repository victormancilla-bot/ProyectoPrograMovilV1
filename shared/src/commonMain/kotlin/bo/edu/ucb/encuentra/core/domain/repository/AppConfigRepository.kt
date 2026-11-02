package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import kotlinx.coroutines.flow.StateFlow

/** Configuración remota de la app (F08). Arranca con los valores por defecto y se actualiza al hacer [refresh]. */
interface AppConfigRepository {
    val config: StateFlow<AppConfig>

    /**
     * Descarga y activa los valores de Remote Config. Si falla, [config] se queda con los que tenía.
     * [force] = descargar de nuevo aunque ya se hizo al abrir la app.
     */
    suspend fun refresh(force: Boolean = false): Result<AppConfig>
}
