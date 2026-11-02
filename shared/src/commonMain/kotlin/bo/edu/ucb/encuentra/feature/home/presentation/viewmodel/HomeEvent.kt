package bo.edu.ucb.encuentra.feature.home.presentation.viewmodel

sealed class HomeEvent {
    /** Botón "Reportar objeto" de la tarjeta "¿Perdiste algo?". */
    data object OnReportObject : HomeEvent()
    data object OnActiveReportsClick : HomeEvent()
    data object OnNewMatchesClick : HomeEvent()
    data object OnMyReportsClick : HomeEvent()
    data object OnFoundItemsClick : HomeEvent()
    data object OnHowItWorksClick : HomeEvent()
}
