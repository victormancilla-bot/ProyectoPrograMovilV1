package bo.edu.ucb.encuentra.feature.matching.domain.model

/**
 * Pesos y umbrales del matching (M02). Vienen de Firebase Remote Config (F08); si no hay Firebase o
 * internet se usa [DEFAULT]. Los pesos son relativos: el score se normaliza a 0–100 aunque no sumen 100.
 */
data class MatchingConfig(
    val colorWeight: Double = 30.0,
    val brandWeight: Double = 20.0,
    val zoneWeight: Double = 20.0,
    val dateWeight: Double = 10.0,
    val textWeight: Double = 20.0,
    /** Debajo de este score no se guarda la coincidencia. */
    val minScore: Int = 30,
    /** Máximo de candidatos que se proponen por reporte (los mejores). */
    val topN: Int = 5,
    /** Días después del final del rango de pérdida en los que todavía puede aparecer el objeto. */
    val maxDaysToFind: Int = 30
) {
    val totalWeight: Double get() = colorWeight + brandWeight + zoneWeight + dateWeight + textWeight

    /** Valores de Remote Config fuera de rango (negativos, todo en 0) vuelven a los por defecto. */
    fun sanitized(): MatchingConfig {
        val weights = listOf(colorWeight, brandWeight, zoneWeight, dateWeight, textWeight)
        if (weights.any { it < 0 || it.isNaN() } || totalWeight <= 0.0) return DEFAULT
        return copy(
            minScore = minScore.coerceIn(0, 100),
            topN = topN.coerceAtLeast(1),
            maxDaysToFind = maxDaysToFind.coerceAtLeast(0)
        )
    }

    companion object {
        val DEFAULT = MatchingConfig()
    }
}
