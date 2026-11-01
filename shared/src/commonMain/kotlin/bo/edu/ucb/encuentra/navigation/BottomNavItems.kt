package bo.edu.ucb.encuentra.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import bo.edu.ucb.encuentra.core.domain.model.Role

data class BottomNavItem(
    val label: String,
    val route: NavRoute,
    val icon: ImageVector
)

object BottomNavItems {

    val student = listOf(
        BottomNavItem("Inicio", NavRoute.Home, Icons.Filled.Home),
        BottomNavItem("Buscar", NavRoute.FoundCatalog, Icons.Filled.Search),
        BottomNavItem("Reportar", NavRoute.ReportWizard, Icons.Filled.AddCircle),
        BottomNavItem("Alertas", NavRoute.Alerts, Icons.Filled.Notifications),
        BottomNavItem("Perfil", NavRoute.Profile, Icons.Filled.Person)
    )

    val staff = listOf(
        BottomNavItem("Panel", NavRoute.Dashboard, Icons.Filled.Dashboard),
        BottomNavItem("Inventario", NavRoute.Inventory, Icons.Filled.Inventory2),
        BottomNavItem("Reclamos", NavRoute.Claims, Icons.Filled.Assignment),
        BottomNavItem("Alertas", NavRoute.Alerts, Icons.Filled.Notifications),
        BottomNavItem("Perfil", NavRoute.Profile, Icons.Filled.Person)
    )

    fun forRole(role: Role): List<BottomNavItem> = if (role.isStaff) staff else student
}
