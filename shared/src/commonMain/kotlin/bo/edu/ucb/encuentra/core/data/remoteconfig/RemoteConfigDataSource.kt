package bo.edu.ucb.encuentra.core.data.remoteconfig

import bo.edu.ucb.encuentra.core.util.safeCall
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

/** Nombres de TODOS los parámetros que se crean en la consola de Firebase (Remote Config). */
object RemoteConfigKeys {
    // F08 · parámetros de la oficina
    const val RETENTION_DAYS = "retention_days"
    const val MATCH_THRESHOLD = "match_threshold"
    const val AI_SUGGESTIONS_ENABLED = "ai_suggestions_enabled"
    const val SUPPORT_CONTACT = "support_contact"
    const val MAINTENANCE_MODE = "maintenance_mode"

    // M02 · pesos del matching (el umbral es MATCH_THRESHOLD)
    const val WEIGHT_COLOR = "matching_weight_color"
    const val WEIGHT_BRAND = "matching_weight_brand"
    const val WEIGHT_ZONE = "matching_weight_zone"
    const val WEIGHT_DATE = "matching_weight_date"
    const val WEIGHT_TEXT = "matching_weight_text"
    const val TOP_N = "matching_top_n"
    const val MAX_DAYS_TO_FIND = "matching_max_days_to_find"

    /**
     * Valores por defecto OFFLINE de todos los parámetros. Firebase `setDefaults` reemplaza el mapa completo, por
     * eso se registran todos juntos y en un solo lugar. Deben coincidir con AppConfig.DEFAULT y MatchingConfig.DEFAULT
     * (lo verifica RemoteConfigTest).
     */
    val DEFAULTS: Map<String, Any> = mapOf(
        RETENTION_DAYS to 90L,
        MATCH_THRESHOLD to 30L,
        AI_SUGGESTIONS_ENABLED to false,
        SUPPORT_CONTACT to "sistemas@ucb.edu.bo",
        MAINTENANCE_MODE to false,
        WEIGHT_COLOR to 30.0,
        WEIGHT_BRAND to 20.0,
        WEIGHT_ZONE to 20.0,
        WEIGHT_DATE to 10.0,
        WEIGHT_TEXT to 20.0,
        TOP_N to 5L,
        MAX_DAYS_TO_FIND to 30L
    )
}

/** Lectura de los parámetros ya activados (descargados ahora, guardados de antes o por defecto). */
interface RemoteValues {
    /** true si en esta sesión se descargaron valores nuevos de Firebase. */
    val fromServer: Boolean
    fun long(key: String): Long
    fun double(key: String): Double
    fun boolean(key: String): Boolean
    fun string(key: String): String
}

/** Descarga de Remote Config; interfaz para poder probar F08 y el matching sin Firebase. */
interface RemoteConfigDataSource {
    /** [force] = volver a descargar aunque ya se haya hecho en esta sesión ("Reintentar" del mantenimiento). */
    suspend fun fetch(force: Boolean = false): Result<RemoteValues>
}

/**
 * Firebase Remote Config, como `RemoteConfigDataSource` de la feature remoteconfig de ucbp1: valores por defecto,
 * `fetchAndActivate` y, si algo falla (sin internet, o sin `google-services.json` todavía), los valores por defecto.
 * Se descarga UNA vez por sesión (al abrir la app); F08 y el matching (M02) comparten el resultado.
 */
class FirebaseRemoteConfigDataSource : RemoteConfigDataSource {

    private val mutex = Mutex()
    private var cached: RemoteValues? = null

    override suspend fun fetch(force: Boolean): Result<RemoteValues> = mutex.withLock {
        if (!force) cached?.let { return@withLock Result.success(it) }
        safeCall {
            val config = Firebase.remoteConfig
            // 0 segundos = cada apertura de la app pide valores nuevos (útil para la demo y para cambios urgentes
            // como maintenance_mode). Como se descarga una vez por sesión, no se pasa el límite de Firebase.
            config.settings { minimumFetchInterval = 0.seconds }
            config.setDefaults(*RemoteConfigKeys.DEFAULTS.toList().toTypedArray())
            // Sin internet seguimos con los valores guardados de la última vez o los por defecto.
            val fromServer = safeCall { config.fetchAndActivate() }.isSuccess
            FirebaseValues(config, fromServer).also { cached = it }
        }
    }

    private class FirebaseValues(private val config: FirebaseRemoteConfig, override val fromServer: Boolean) : RemoteValues {
        override fun long(key: String) = config.getValue(key).asLong()
        override fun double(key: String) = config.getValue(key).asDouble()
        override fun boolean(key: String) = config.getValue(key).asBoolean()
        override fun string(key: String) = config.getValue(key).asString()
    }
}
