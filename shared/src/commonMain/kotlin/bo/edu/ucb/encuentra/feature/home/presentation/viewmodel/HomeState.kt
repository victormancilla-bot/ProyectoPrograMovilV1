package bo.edu.ucb.encuentra.feature.home.presentation.viewmodel

data class HomeState(
    /** "¡Hola, Mateo!" con el primer nombre del usuario. */
    val greeting: String = "",
    /** "Ingeniería • Sede La Paz": carrera y sede del usuario (no fija). */
    val subtitle: String = "",
    val activeReports: Int = 0,
    val newMatches: Int = 0,
    val loading: Boolean = true
) {
    val activeReportsText: String
        get() = if (activeReports == 1) "1 reporte activo" else "$activeReports reportes activos"

    val newMatchesText: String
        get() = if (newMatches == 1) "1 coincidencia nueva" else "$newMatches coincidencias nuevas"

    /** Punto rojo sobre el ícono de coincidencias (Figma), solo si hay alguna sin ver. */
    val hasNewMatches: Boolean get() = newMatches > 0
}
