package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import edu.ucb.ds.components.navigation.BottomNavBar
import edu.ucb.ds.components.navigation.BottomNavCenterAction
import edu.ucb.ds.components.navigation.BottomNavItem

/**
 * Scaffold con la barra inferior del rol (BottomNavBar del Design System, F04).
 * Si [tabs] está vacío (Login, wizard, detalles) no se muestra la barra.
 */
@Composable
fun MainScaffold(
    tabs: List<RoleTab>,
    centerAction: RoleTab?,
    currentDestination: NavDestination?,
    onTabClick: (NavRoute) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (tabs.isNotEmpty()) {
                BottomNavBar(
                    items = tabs.map { tab ->
                        BottomNavItem(
                            label = tab.label,
                            icon = tab.icon,
                            selected = currentDestination.isOn(tab.route),
                            onClick = { onTabClick(tab.route) }
                        )
                    },
                    centerAction = centerAction?.let { action ->
                        BottomNavCenterAction(action.icon, action.label) { onTabClick(action.route) }
                    }
                )
            }
        },
        content = content
    )
}

/** ¿El destino actual (o uno de sus padres) es [route]? */
fun NavDestination?.isOn(route: NavRoute): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true
