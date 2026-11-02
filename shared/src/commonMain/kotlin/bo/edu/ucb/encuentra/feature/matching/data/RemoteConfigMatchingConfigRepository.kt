package bo.edu.ucb.encuentra.feature.matching.data

import bo.edu.ucb.encuentra.core.util.safeCall
import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchingConfig
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingConfigRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlin.time.Duration.Companion.hours

/** Nombres de los parámetros que se crean en la consola de Firebase (Remote Config, F08). */
object MatchingConfigKeys {
    const val WEIGHT_COLOR = "matching_weight_color"
    const val WEIGHT_BRAND = "matching_weight_brand"
    const val WEIGHT_ZONE = "matching_weight_zone"
    const val WEIGHT_DATE = "matching_weight_date"
    const val WEIGHT_TEXT = "matching_weight_text"
    const val MIN_SCORE = "matching_min_score"
    const val TOP_N = "matching_top_n"
    const val MAX_DAYS_TO_FIND = "matching_max_days_to_find"
}

/**
 * Pesos del matching desde Remote Config, como `RemoteConfigDataSource` de ucbp1: valores por defecto,
 * `fetchAndActivate` y, si algo falla (sin internet o sin `google-services.json` todavía), los por defecto.
 * Se lee una vez por sesión de la app; un cambio en la consola se aplica al volver a abrirla.
 */
class RemoteConfigMatchingConfigRepository : MatchingConfigRepository {

    private var cached: MatchingConfig? = null

    override suspend fun get(): MatchingConfig {
        cached?.let { return it }
        val config = safeCall { fetch() }.getOrDefault(MatchingConfig.DEFAULT).sanitized()
        cached = config
        return config
    }

    private suspend fun fetch(): MatchingConfig {
        val remote = Firebase.remoteConfig
        val defaults = MatchingConfig.DEFAULT
        remote.settings { minimumFetchInterval = 12.hours }
        remote.setDefaults(
            MatchingConfigKeys.WEIGHT_COLOR to defaults.colorWeight,
            MatchingConfigKeys.WEIGHT_BRAND to defaults.brandWeight,
            MatchingConfigKeys.WEIGHT_ZONE to defaults.zoneWeight,
            MatchingConfigKeys.WEIGHT_DATE to defaults.dateWeight,
            MatchingConfigKeys.WEIGHT_TEXT to defaults.textWeight,
            MatchingConfigKeys.MIN_SCORE to defaults.minScore.toLong(),
            MatchingConfigKeys.TOP_N to defaults.topN.toLong(),
            MatchingConfigKeys.MAX_DAYS_TO_FIND to defaults.maxDaysToFind.toLong()
        )
        // Sin internet seguimos con los valores guardados / por defecto.
        safeCall { remote.fetchAndActivate() }
        return remote.read()
    }

    private fun FirebaseRemoteConfig.read() = MatchingConfig(
        colorWeight = getValue(MatchingConfigKeys.WEIGHT_COLOR).asDouble(),
        brandWeight = getValue(MatchingConfigKeys.WEIGHT_BRAND).asDouble(),
        zoneWeight = getValue(MatchingConfigKeys.WEIGHT_ZONE).asDouble(),
        dateWeight = getValue(MatchingConfigKeys.WEIGHT_DATE).asDouble(),
        textWeight = getValue(MatchingConfigKeys.WEIGHT_TEXT).asDouble(),
        minScore = getValue(MatchingConfigKeys.MIN_SCORE).asLong().toInt(),
        topN = getValue(MatchingConfigKeys.TOP_N).asLong().toInt(),
        maxDaysToFind = getValue(MatchingConfigKeys.MAX_DAYS_TO_FIND).asLong().toInt()
    )
}
