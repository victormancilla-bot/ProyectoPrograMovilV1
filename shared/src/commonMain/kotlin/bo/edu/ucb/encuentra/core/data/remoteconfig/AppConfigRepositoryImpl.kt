package bo.edu.ucb.encuentra.core.data.remoteconfig

import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import bo.edu.ucb.encuentra.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Configuración de la app desde Remote Config (F08); mientras no llega, los valores por defecto offline. */
class AppConfigRepositoryImpl(private val dataSource: RemoteConfigDataSource) : AppConfigRepository {

    private val state = MutableStateFlow(AppConfig.DEFAULT)
    override val config: StateFlow<AppConfig> = state.asStateFlow()

    override suspend fun refresh(force: Boolean): Result<AppConfig> =
        dataSource.fetch(force)
            .map { it.toAppConfig().sanitized() }
            .onSuccess { state.value = it }

    companion object {
        fun RemoteValues.toAppConfig() = AppConfig(
            retentionDays = long(RemoteConfigKeys.RETENTION_DAYS).toInt(),
            matchThreshold = long(RemoteConfigKeys.MATCH_THRESHOLD).toInt(),
            aiSuggestionsEnabled = boolean(RemoteConfigKeys.AI_SUGGESTIONS_ENABLED),
            supportContact = string(RemoteConfigKeys.SUPPORT_CONTACT),
            maintenanceMode = boolean(RemoteConfigKeys.MAINTENANCE_MODE),
            fromServer = fromServer
        )
    }
}
