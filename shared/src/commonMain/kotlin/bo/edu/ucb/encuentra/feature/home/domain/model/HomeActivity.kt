package bo.edu.ucb.encuentra.feature.home.domain.model

/** Sección "Actividad" del Home: reportes en curso y coincidencias que el estudiante todavía no vio. */
data class HomeActivity(
    val activeReports: Int,
    val newMatches: Int
) {
    companion object {
        val EMPTY = HomeActivity(activeReports = 0, newMatches = 0)
    }
}
