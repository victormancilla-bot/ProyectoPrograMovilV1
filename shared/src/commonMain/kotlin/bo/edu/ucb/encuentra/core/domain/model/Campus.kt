package bo.edu.ucb.encuentra.core.domain.model

/** Sede de la UCB del usuario y de cada objeto. El header muestra "Sede La Paz" (configurable, no fija). */
enum class Campus(val label: String) {
    LA_PAZ("Sede La Paz"),
    COCHABAMBA("Sede Cochabamba"),
    SANTA_CRUZ("Sede Santa Cruz"),
    TARIJA("Sede Tarija")
}
