package bo.edu.ucb.encuentra.navigation

import kotlinx.serialization.Serializable

// ⚠️ Agregar o cambiar rutas se coordina con el dueño de F05 (Integrante B).
@Serializable
sealed class NavRoute {

    @Serializable
    data object Login : NavRoute()

    // ----- Estudiante -----
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object ReportWizard : NavRoute()

    @Serializable
    data object MyReports : NavRoute()

    @Serializable
    data object FoundCatalog : NavRoute()

    @Serializable
    data object HowItWorks : NavRoute()

    @Serializable
    data object MatchList : NavRoute()

    @Serializable
    data class MatchDetail(val matchId: String) : NavRoute()

    @Serializable
    data class Verify(val matchId: String) : NavRoute()

    // ----- Staff / Admin -----
    @Serializable
    data object Dashboard : NavRoute()

    @Serializable
    data object FoundCapture : NavRoute()

    @Serializable
    data object FoundForm : NavRoute()

    @Serializable
    data object Inventory : NavRoute()

    @Serializable
    data object Claims : NavRoute()

    @Serializable
    data class ClaimDetail(val claimId: String) : NavRoute()

    @Serializable
    data class Delivery(val claimId: String) : NavRoute()

    @Serializable
    data object Audit : NavRoute()

    // ----- Comunes -----
    @Serializable
    data object Alerts : NavRoute()

    @Serializable
    data object Profile : NavRoute()

    // ----- Desarrollo (se quitan antes del entregable final) -----
    @Serializable
    data object DevMenu : NavRoute()

    @Serializable
    data object DesignSystemDemo : NavRoute()
}
