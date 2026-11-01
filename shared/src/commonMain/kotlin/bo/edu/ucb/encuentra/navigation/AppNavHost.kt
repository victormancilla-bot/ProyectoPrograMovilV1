package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.alerts.presentation.screen.AlertsScreen
import bo.edu.ucb.encuentra.feature.audit.presentation.screen.AuditScreen
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.ClaimDetailScreen
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.ClaimsScreen
import bo.edu.ucb.encuentra.feature.dashboard.presentation.screen.DashboardScreen
import bo.edu.ucb.encuentra.feature.delivery.presentation.screen.DeliveryScreen
import bo.edu.ucb.encuentra.feature.designsystem.presentation.screen.DesignSystemDemoScreen
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.screen.FoundCaptureScreen
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen.FoundCatalogScreen
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.screen.HowItWorksScreen
import bo.edu.ucb.encuentra.feature.foundform.presentation.screen.FoundFormScreen
import bo.edu.ucb.encuentra.feature.home.presentation.screen.HomeScreen
import bo.edu.ucb.encuentra.feature.inventory.presentation.screen.InventoryScreen
import bo.edu.ucb.encuentra.feature.login.presentation.screen.LoginScreen
import bo.edu.ucb.encuentra.feature.matching.presentation.screen.MatchDetailScreen
import bo.edu.ucb.encuentra.feature.matching.presentation.screen.MatchListScreen
import bo.edu.ucb.encuentra.feature.myreports.presentation.screen.MyReportsScreen
import bo.edu.ucb.encuentra.feature.profile.presentation.screen.ProfileScreen
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen.ReportWizardScreen
import bo.edu.ucb.encuentra.feature.verify.presentation.screen.VerifyScreen
import org.koin.compose.koinInject

/**
 * Navegación de la app (F05): todas las rutas pasan por [RouteGuard] y la barra inferior depende del rol.
 */
@Composable
fun AppNavHost(
    getCurrentUser: GetCurrentUserUseCase = koinInject()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val user = getCurrentUser()

    // Con sesión se abre directo el home de su rol; sin sesión, Login.
    val startDestination = remember { getCurrentUser()?.let { RouteGuard.homeFor(it.role) } ?: NavRoute.Login }

    // La barra solo se muestra en las pestañas del rol (no en Login, detalles ni el wizard).
    val tabs = user?.let { BottomNavItems.forRole(it.role) }.orEmpty()
    val showBottomBar = tabs.any { it.route != NavRoute.ReportWizard && currentDestination.isOn(it.route) }

    MainScaffold(
        items = if (showBottomBar) tabs else emptyList(),
        currentDestination = currentDestination,
        onItemClick = { route -> navController.navigateToTab(route, user?.role?.let(RouteGuard::homeFor)) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            appRoutes(navController, getCurrentUser)
        }
    }
}

private fun NavGraphBuilder.appRoutes(navController: NavHostController, getCurrentUser: GetCurrentUserUseCase) {
    val back: () -> Unit = { navController.popBackStack() }

    guarded<NavRoute.Login>(navController, getCurrentUser) {
        LoginScreen(
            onNavigateToHome = {
                val home = getCurrentUser()?.let { RouteGuard.homeFor(it.role) } ?: NavRoute.Login
                navController.navigate(home) {
                    popUpTo<NavRoute.Login> { inclusive = true }
                }
            },
            onShowToast = { message -> println("Toast: $message") }
        )
    }

    // Estudiante
    guarded<NavRoute.Home>(navController, getCurrentUser) { HomeScreen(onNavigateBack = back) }
    guarded<NavRoute.ReportWizard>(navController, getCurrentUser) {
        // Cancelar saca el wizard del back stack: su ViewModel (el borrador) se destruye.
        ReportWizardScreen(onCancel = { navController.popBackStack(NavRoute.ReportWizard, inclusive = true) })
    }
    guarded<NavRoute.MyReports>(navController, getCurrentUser) { MyReportsScreen(onNavigateBack = back) }
    guarded<NavRoute.FoundCatalog>(navController, getCurrentUser) { FoundCatalogScreen(onNavigateBack = back) }
    guarded<NavRoute.HowItWorks>(navController, getCurrentUser) { HowItWorksScreen(onNavigateBack = back) }
    guarded<NavRoute.MatchList>(navController, getCurrentUser) { MatchListScreen(onNavigateBack = back) }
    guarded<NavRoute.MatchDetail>(navController, getCurrentUser) { route ->
        MatchDetailScreen(matchId = route.matchId, onNavigateBack = back)
    }
    guarded<NavRoute.Verify>(navController, getCurrentUser) { route ->
        VerifyScreen(matchId = route.matchId, onNavigateBack = back)
    }

    // Staff / Admin
    guarded<NavRoute.Dashboard>(navController, getCurrentUser) { DashboardScreen(onNavigateBack = back) }
    guarded<NavRoute.FoundCapture>(navController, getCurrentUser) { FoundCaptureScreen(onNavigateBack = back) }
    guarded<NavRoute.FoundForm>(navController, getCurrentUser) { FoundFormScreen(onNavigateBack = back) }
    guarded<NavRoute.Inventory>(navController, getCurrentUser) { InventoryScreen(onNavigateBack = back) }
    guarded<NavRoute.Claims>(navController, getCurrentUser) { ClaimsScreen(onNavigateBack = back) }
    guarded<NavRoute.ClaimDetail>(navController, getCurrentUser) { route ->
        ClaimDetailScreen(claimId = route.claimId, onNavigateBack = back)
    }
    guarded<NavRoute.Delivery>(navController, getCurrentUser) { route ->
        DeliveryScreen(claimId = route.claimId, onNavigateBack = back)
    }
    guarded<NavRoute.Audit>(navController, getCurrentUser) { AuditScreen(onNavigateBack = back) }

    // Comunes
    guarded<NavRoute.Alerts>(navController, getCurrentUser) { AlertsScreen(onNavigateBack = back) }
    guarded<NavRoute.Profile>(navController, getCurrentUser) {
        ProfileScreen(
            onNavigateToLogin = {
                navController.navigate(NavRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            onOpenDevMenu = { navController.navigate(NavRoute.DevMenu) },
            onNavigateBack = back
        )
    }

    // Desarrollo
    guarded<NavRoute.DevMenu>(navController, getCurrentUser) {
        DevMenuScreen(onNavigate = { route -> navController.navigate(route) })
    }
    guarded<NavRoute.DesignSystemDemo>(navController, getCurrentUser) { DesignSystemDemoScreen(onNavigateBack = back) }
}

/**
 * Igual que `composable<T>`, pero antes de mostrar la pantalla pregunta a [RouteGuard].
 * Si no puede entrar, limpia el back stack y lo manda a Login o al home de su rol.
 */
private inline fun <reified T : NavRoute> NavGraphBuilder.guarded(
    navController: NavHostController,
    getCurrentUser: GetCurrentUserUseCase,
    deepLinks: List<NavDeepLink> = emptyList(),
    crossinline content: @Composable (T) -> Unit
) {
    composable<T>(deepLinks = deepLinks) { entry ->
        val route = entry.toRoute<T>()
        val redirect = RouteGuard.redirect(route, getCurrentUser())
        if (redirect == null) {
            content(route)
        } else {
            LaunchedEffect(redirect) {
                navController.navigate(redirect) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }
}

/** Cambio de pestaña: no apila pestañas y recuerda el estado de cada una. */
private fun NavHostController.navigateToTab(route: NavRoute, home: NavRoute?) {
    navigate(route) {
        if (home != null) {
            popUpTo(home) { saveState = true }
        }
        launchSingleTop = true
        restoreState = route != NavRoute.ReportWizard // el wizard siempre empieza de cero
    }
}
