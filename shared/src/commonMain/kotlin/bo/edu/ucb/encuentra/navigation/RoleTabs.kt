package bo.edu.ucb.encuentra.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import bo.edu.ucb.encuentra.core.domain.model.Role

/** Una pestaña de la barra inferior: qué ruta abre y con qué ícono. */
data class RoleTab(
    val label: String,
    val route: NavRoute,
    val icon: ImageVector
)

/** Pestañas por rol (Figma: Home Estudiante y Dashboard Administrador). */
object RoleTabs {

    val student = listOf(
        RoleTab("Inicio", NavRoute.Home, Icons.Filled.Home),
        RoleTab("Buscar", NavRoute.FoundCatalog, Icons.Filled.Search),
        RoleTab("Alertas", NavRoute.Alerts, Icons.Filled.Notifications),
        RoleTab("Perfil", NavRoute.Profile, Icons.Filled.Person)
    )

    /** FAB "+" central del estudiante: abre el wizard de reporte. */
    val studentReport = RoleTab("Reportar", NavRoute.ReportWizard, Icons.Filled.Add)

    val staff = listOf(
        RoleTab("Panel", NavRoute.Dashboard, Icons.Filled.Dashboard),
        RoleTab("Inventario", NavRoute.Inventory, Icons.Filled.Inventory2),
        RoleTab("Reclamos", NavRoute.Claims, Icons.Filled.Assignment),
        RoleTab("Alertas", NavRoute.Alerts, Icons.Filled.Notifications),
        RoleTab("Perfil", NavRoute.Profile, Icons.Filled.Person)
    )

    fun forRole(role: Role): List<RoleTab> = if (role.isStaff) staff else student

    fun centerActionFor(role: Role): RoleTab? = if (role.isStaff) null else studentReport
}
