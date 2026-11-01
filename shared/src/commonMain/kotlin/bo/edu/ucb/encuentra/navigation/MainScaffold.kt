package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import edu.ucb.ds.theme.UcbEncuentraTheme

/**
 * Scaffold con la barra inferior del rol. F04 puede reemplazar la NavigationBar por su BottomNavBar con FAB.
 * Si [items] está vacío (Login, wizard, detalles) no se muestra la barra.
 */
@Composable
fun MainScaffold(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onItemClick: (NavRoute) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (items.isNotEmpty()) {
                NavigationBar(containerColor = UcbEncuentraTheme.colors.background) {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination.isOn(item.route),
                            onClick = { onItemClick(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = UcbEncuentraTheme.typography.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = UcbEncuentraTheme.colors.onSecondary,
                                selectedTextColor = UcbEncuentraTheme.colors.primary,
                                indicatorColor = UcbEncuentraTheme.colors.secondary
                            )
                        )
                    }
                }
            }
        },
        content = content
    )
}

/** ¿El destino actual (o uno de sus padres) es [route]? */
fun NavDestination?.isOn(route: NavRoute): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true
