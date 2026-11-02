package bo.edu.ucb.encuentra.feature.matching.data

import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteConfigDataSource
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteConfigKeys
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteValues
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingConfigRepository

/**
 * Pesos del matching desde Remote Config (M02). Usa el mismo [RemoteConfigDataSource] que F08 (una sola
 * descarga por sesión y un solo `setDefaults`); el umbral es el parámetro `match_threshold` de F08.
 * Si Firebase no está disponible (sin `google-services.json` todavía), los valores por defecto.
 */
class RemoteConfigMatchingConfigRepository(private val dataSource: RemoteConfigDataSource) : MatchingConfigRepository {

    override suspend fun get(): MatchingConfig =
        dataSource.fetch().map { it.toMatchingConfig() }.getOrDefault(MatchingConfig.DEFAULT).sanitized()

    private fun RemoteValues.toMatchingConfig() = MatchingConfig(
        colorWeight = double(RemoteConfigKeys.WEIGHT_COLOR),
        brandWeight = double(RemoteConfigKeys.WEIGHT_BRAND),
        zoneWeight = double(RemoteConfigKeys.WEIGHT_ZONE),
        dateWeight = double(RemoteConfigKeys.WEIGHT_DATE),
        textWeight = double(RemoteConfigKeys.WEIGHT_TEXT),
        minScore = long(RemoteConfigKeys.MATCH_THRESHOLD).toInt(),
        topN = long(RemoteConfigKeys.TOP_N).toInt(),
        maxDaysToFind = long(RemoteConfigKeys.MAX_DAYS_TO_FIND).toInt()
    )
}
