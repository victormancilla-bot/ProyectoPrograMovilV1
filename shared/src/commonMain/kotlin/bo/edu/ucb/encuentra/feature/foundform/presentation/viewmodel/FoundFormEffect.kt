package bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel

sealed class FoundFormEffect {
    /** El objeto quedó guardado como DISPONIBLE: la pantalla va al inventario. */
    data class Registered(val title: String) : FoundFormEffect()
}
