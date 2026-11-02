package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.data.remoteconfig.AppConfigRepositoryImpl
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteConfigDataSource
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteConfigKeys
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteValues
import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import bo.edu.ucb.encuentra.core.domain.repository.AppConfigRepository
import bo.edu.ucb.encuentra.feature.matching.data.RemoteConfigMatchingConfigRepository
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Configuración fija para los tests de las features que leen F08. */
internal class FixedAppConfig(initial: AppConfig = AppConfig.DEFAULT) : AppConfigRepository {
    private val state = MutableStateFlow(initial)
    override val config: StateFlow<AppConfig> = state.asStateFlow()
    override suspend fun refresh(force: Boolean) = Result.success(state.value)
    fun update(change: (AppConfig) -> AppConfig) = state.update(change)
}

/** Valores "de la consola": los por defecto más lo que el test cambie. */
private class ConsoleValues(
    overrides: Map<String, Any> = emptyMap(),
    override val fromServer: Boolean = true
) : RemoteValues {
    private val values = RemoteConfigKeys.DEFAULTS + overrides
    override fun long(key: String) = (values.getValue(key) as Number).toLong()
    override fun double(key: String) = (values.getValue(key) as Number).toDouble()
    override fun boolean(key: String) = values.getValue(key) as Boolean
    override fun string(key: String) = values.getValue(key) as String
}

private class FakeRemoteConfig(var result: Result<RemoteValues>) : RemoteConfigDataSource {
    val forced = mutableListOf<Boolean>()
    override suspend fun fetch(force: Boolean): Result<RemoteValues> {
        forced += force
        return result
    }
}

class RemoteConfigTest {

    @Test
    fun offlineDefaultsMatchTheAppAndMatchingDefaults() = runTest {
        val offline = ConsoleValues(fromServer = false)
        val repository = AppConfigRepositoryImpl(FakeRemoteConfig(Result.success(offline)))

        assertEquals(AppConfig.DEFAULT, repository.refresh().getOrThrow())
        assertEquals(MatchingConfig.DEFAULT, RemoteConfigMatchingConfigRepository(FakeRemoteConfig(Result.success(offline))).get())
        // Las 5 claves de la tarjeta F08 tienen valor por defecto.
        assertTrue(
            listOf("retention_days", "match_threshold", "ai_suggestions_enabled", "support_contact", "maintenance_mode")
                .all { it in RemoteConfigKeys.DEFAULTS }
        )
    }

    @Test
    fun valuesFromTheConsoleAreApplied() = runTest {
        val console = ConsoleValues(
            mapOf(
                RemoteConfigKeys.RETENTION_DAYS to 60L,
                RemoteConfigKeys.MATCH_THRESHOLD to 45L,
                RemoteConfigKeys.AI_SUGGESTIONS_ENABLED to true,
                RemoteConfigKeys.SUPPORT_CONTACT to "objetosperdidos@ucb.edu.bo",
                RemoteConfigKeys.MAINTENANCE_MODE to true
            )
        )
        val repository = AppConfigRepositoryImpl(FakeRemoteConfig(Result.success(console)))

        repository.refresh().getOrThrow()

        assertEquals(
            AppConfig(60, 45, aiSuggestionsEnabled = true, supportContact = "objetosperdidos@ucb.edu.bo", maintenanceMode = true, fromServer = true),
            repository.config.value
        )
        // El umbral del matching es el mismo parámetro.
        assertEquals(45, RemoteConfigMatchingConfigRepository(FakeRemoteConfig(Result.success(console))).get().minScore)
    }

    @Test
    fun aFailedFetchKeepsTheDefaults() = runTest {
        val source = FakeRemoteConfig(Result.failure(IllegalStateException("Default FirebaseApp is not initialized")))
        val repository = AppConfigRepositoryImpl(source)

        assertTrue(repository.refresh().isFailure)
        assertEquals(AppConfig.DEFAULT, repository.config.value)
        assertFalse(repository.config.value.maintenanceMode)
        assertEquals(MatchingConfig.DEFAULT, RemoteConfigMatchingConfigRepository(source).get())
    }

    @Test
    fun outOfRangeValuesAreCorrected() = runTest {
        val console = ConsoleValues(
            mapOf(
                RemoteConfigKeys.RETENTION_DAYS to 0L,
                RemoteConfigKeys.MATCH_THRESHOLD to 500L,
                RemoteConfigKeys.SUPPORT_CONTACT to "   "
            )
        )
        val repository = AppConfigRepositoryImpl(FakeRemoteConfig(Result.success(console)))

        val config = repository.refresh().getOrThrow()

        assertEquals(90, config.retentionDays)
        assertEquals(100, config.matchThreshold)
        assertEquals("sistemas@ucb.edu.bo", config.supportContact)
    }

    @Test
    fun retryForcesANewDownload() = runTest {
        val source = FakeRemoteConfig(Result.success(ConsoleValues(mapOf(RemoteConfigKeys.MAINTENANCE_MODE to true))))
        val repository = AppConfigRepositoryImpl(source)
        repository.refresh()
        assertTrue(repository.config.value.maintenanceMode)

        // La oficina apaga el mantenimiento y el usuario toca "Reintentar".
        source.result = Result.success(ConsoleValues())
        repository.refresh(force = true)

        assertEquals(listOf(false, true), source.forced)
        assertFalse(repository.config.value.maintenanceMode)
    }
}
