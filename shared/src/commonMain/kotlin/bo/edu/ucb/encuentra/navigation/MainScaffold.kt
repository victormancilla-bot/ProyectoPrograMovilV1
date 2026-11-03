package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import bo.edu.ucb.encuentra.core.presentation.AppSnackbar
import bo.edu.ucb.encuentra.core.presentation.OfflineBanner
import edu.ucb.ds.components.navigation.BottomNavBar
import edu.ucb.ds.components.navigation.BottomNavCenterAction
import edu.ucb.ds.components.navigation.BottomNavItem

/**
 * Scaffold con la barra inferior del rol (BottomNavBar del Design System, F04).
 * Si [tabs] está vacío (Login, wizard, detalles) no se muestra la barra.
 * F11: un único SnackbarHost para los mensajes de toda la app y el banner "sin conexión" sobre la barra.
 */
@Composable
fun MainScaffold(
    tabs: List<RoleTab>,
    centerAction: RoleTab?,
    currentDestination: NavDestination?,
    onTabClick: (NavRoute) -> Unit,
    /** N04: contador rojo por pestaña (Alertas sin leer). */
    badges: Map<NavRoute, Int> = emptyMap(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isOnline: Boolean = true,
    onRetryConnection: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        // Sin insets del sistema: el CurvedHeader se pinta detrás de la barra de estado y la BottomNavBar
        // ya respeta la barra de navegación. Cada pantalla maneja su propio espacio arriba.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> AppSnackbar(data) } },
        bottomBar = {
            Column {
                if (!isOnline) {
                    OfflineBanner(
                        onRetry = onRetryConnection,
                        // Sin barra inferior (Login, wizard), el banner respeta la barra de navegación del sistema.
                        modifier = if (tabs.isEmpty()) Modifier.navigationBarsPadding() else Modifier
                    )
                }
                if (tabs.isNotEmpty()) {
                    BottomNavBar(
                        items = tabs.map { tab ->
                            BottomNavItem(
                                label = tab.label,
                                icon = tab.icon,
                                selected = currentDestination.isOn(tab.route),
                                onClick = { onTabClick(tab.route) },
                                badgeCount = badges[tab.route] ?: 0
                            )
                        },
                        centerAction = centerAction?.let { action ->
                            BottomNavCenterAction(action.icon, action.label) { onTabClick(action.route) }
                        }
                    )
                }
            }
        },
        content = content
    )
}

/** ¿El destino actual (o uno de sus padres) es [route]? */
fun NavDestination?.isOn(route: NavRoute): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true
