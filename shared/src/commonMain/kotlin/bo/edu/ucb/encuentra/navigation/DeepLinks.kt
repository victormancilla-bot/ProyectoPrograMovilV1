package bo.edu.ucb.encuentra.navigation

/**
 * Deep links de la app. N02 (push FCM) usa [match] como dato `link` de la notificación.
 * El esquema y host también están en el intent-filter de androidApp/AndroidManifest.xml.
 */
object DeepLinks {
    const val SCHEME = "ucbencuentra"
    const val MATCH_BASE = "$SCHEME://match"

    /** ucbencuentra://retention → NavRoute.Retention (aviso diario de D04 al staff). */
    const val RETENTION = "$SCHEME://retention"

    /** ucbencuentra://match/{matchId} → NavRoute.MatchDetail(matchId) */
    fun match(matchId: String): String = "$MATCH_BASE/$matchId"
}
