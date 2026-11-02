package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import bo.edu.ucb.encuentra.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.StateFlow

/** Valores actuales de la configuración remota (F08): los por defecto hasta que llega el fetch. */
class GetAppConfigUseCase(private val repository: AppConfigRepository) {
    operator fun invoke(): AppConfig = repository.config.value
}

/** La configuración en vivo: la pantalla de mantenimiento reacciona apenas llega el fetch. */
class ObserveAppConfigUseCase(private val repository: AppConfigRepository) {
    operator fun invoke(): StateFlow<AppConfig> = repository.config
}

/**
 * Descarga la configuración (al abrir la app y en "Reintentar" del mantenimiento). Un error (sin internet, sin
 * Firebase) no rompe nada: la app sigue con los valores por defecto o los últimos guardados.
 */
class RefreshAppConfigUseCase(private val repository: AppConfigRepository) {
    suspend operator fun invoke(force: Boolean = false): Result<AppConfig> = repository.refresh(force)
}
