package bo.edu.ucb.encuentra.core.domain.model

/**
 * Parámetros que la oficina de Objetos Perdidos puede cambiar desde Firebase Remote Config sin publicar una
 * versión nueva (F08). Los valores por defecto son los que se usan sin internet o sin `google-services.json`.
 */
data class AppConfig(
    /** Días que un objeto queda en custodia antes de "por vencer" / donación (H03, D04). */
    val retentionDays: Int = 90,
    /** Score mínimo (0-100) para proponer una coincidencia (M01/M02). */
    val matchThreshold: Int = 30,
    /** Sugerencias automáticas de atributos con IA al registrar un hallazgo (H02). */
    val aiSuggestionsEnabled: Boolean = false,
    /** Correo de soporte ("Contacta a Sistemas UCB" del login). */
    val supportContact: String = "sistemas@ucb.edu.bo",
    /** true = la app muestra "En mantenimiento" en vez de las pantallas. */
    val maintenanceMode: Boolean = false,
    /** true si los valores se descargaron de Firebase en esta sesión; false = guardados o por defecto. */
    val fromServer: Boolean = false
) {
    /** Valores fuera de rango en la consola (p. ej. retención 0 o umbral 500) vuelven a uno razonable. */
    fun sanitized(): AppConfig = copy(
        retentionDays = retentionDays.takeIf { it in MIN_RETENTION_DAYS..MAX_RETENTION_DAYS } ?: DEFAULT.retentionDays,
        matchThreshold = matchThreshold.coerceIn(0, MAX_SCORE),
        supportContact = supportContact.trim().ifEmpty { DEFAULT.supportContact }
    )

    companion object {
        val DEFAULT = AppConfig()
        const val MIN_RETENTION_DAYS = 7
        const val MAX_RETENTION_DAYS = 365
        private const val MAX_SCORE = 100
    }
}
