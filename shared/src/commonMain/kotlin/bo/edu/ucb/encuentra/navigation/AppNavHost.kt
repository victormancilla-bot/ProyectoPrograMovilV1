package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.presentation.LocalNotificationsEffect
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveUnreadAlertsCountUseCase
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
import bo.edu.ucb.encuentra.feature.profile.presentation.screen.UserRolesScreen
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.screen.ReportWizardScreen
import bo.edu.ucb.encuentra.feature.verify.presentation.screen.VerifyScreen
import org.koin.compose.koinInject

/**
 * Navegación de la app (F05): todas las rutas pasan por [RouteGuard] y la barra inferior depende del rol.
 */
@Composable
fun AppNavHost(
    getCurrentUser: GetCurrentUserUseCase = koinInject(),
    observeUnreadAlerts: ObserveUnreadAlertsCountUseCase = koinInject()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val user = getCurrentUser()

    // Con sesión se abre directo el home de su rol; sin sesión, Login.
    val startDestination = remember { getCurrentUser()?.let { RouteGuard.homeFor(it.role) } ?: NavRoute.Login }

    // N01: al entrar un usuario se pide el permiso de notificaciones y se muestran sus avisos pendientes.
    LocalNotificationsEffect(userId = user?.id)

    // N04: badge de la pestaña Alertas con las no leídas del usuario actual.
    val unreadAlerts by remember(user?.id) { observeUnreadAlerts(user?.id) }.collectAsState(initial = 0)

    // Deep links que llegan con la app abierta (notificaciones).
    DisposableEffect(navController) {
        ExternalUriHandler.listener = { uri -> navController.openDeepLink(uri) }
        onDispose { ExternalUriHandler.listener = null }
    }

    // La barra solo se muestra en las pestañas del rol (no en Login, detalles ni el wizard).
    val tabs = user?.let { RoleTabs.forRole(it.role) }.orEmpty()
    val showBottomBar = tabs.any { currentDestination.isOn(it.route) }

    MainScaffold(
        tabs = if (showBottomBar) tabs else emptyList(),
        centerAction = user?.let { RoleTabs.centerActionFor(it.role) },
        currentDestination = currentDestination,
        onTabClick = { route -> navController.navigateToTab(route, user?.role?.let(RouteGuard::homeFor)) },
        badges = mapOf(NavRoute.Alerts to unreadAlerts)
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
        val uriHandler = LocalUriHandler.current
        LoginScreen(
            onNavigateToHome = {
                val home = getCurrentUser()?.let { RouteGuard.homeFor(it.role) } ?: NavRoute.Login
                navController.navigate(home) {
                    popUpTo<NavRoute.Login> { inclusive = true }
                }
            },
            onOpenSupport = { uri -> uriHandler.openUri(uri) }
        )
    }

    // Estudiante
    guarded<NavRoute.Home>(navController, getCurrentUser) {
        HomeScreen(
            onNavigateToReportWizard = { navController.navigateToTab(NavRoute.ReportWizard, NavRoute.Home) },
            onNavigateToMyReports = { navController.navigate(NavRoute.MyReports) },
            onNavigateToMatches = { navController.navigate(NavRoute.MatchList) },
            onNavigateToFoundCatalog = { navController.navigateToTab(NavRoute.FoundCatalog, NavRoute.Home) },
            onNavigateToHowItWorks = { navController.navigate(NavRoute.HowItWorks) }
        )
    }
    guarded<NavRoute.ReportWizard>(navController, getCurrentUser) {
        // Cancelar saca el wizard del back stack: su ViewModel (el borrador) se destruye.
        ReportWizardScreen(
            onCancel = { navController.popBackStack(NavRoute.ReportWizard, inclusive = true) },
            // Después de enviar: Mis reportes reemplaza al wizard (atrás no vuelve al formulario enviado).
            onGoToMyReports = {
                navController.navigate(NavRoute.MyReports) { popUpTo<NavRoute.ReportWizard> { inclusive = true } }
            }
        )
    }
    guarded<NavRoute.MyReports>(navController, getCurrentUser) {
        MyReportsScreen(
            onNavigateBack = back,
            onOpenMatch = { matchId -> navController.navigate(NavRoute.MatchDetail(matchId)) },
            onSeeAllMatches = { navController.navigate(NavRoute.MatchList) },
            onReportLost = { navController.navigateToTab(NavRoute.ReportWizard, NavRoute.Home) }
        )
    }
    guarded<NavRoute.FoundCatalog>(navController, getCurrentUser) { FoundCatalogScreen(onNavigateBack = back) }
    guarded<NavRoute.HowItWorks>(navController, getCurrentUser) { HowItWorksScreen(onNavigateBack = back) }
    guarded<NavRoute.MatchList>(navController, getCurrentUser) {
        MatchListScreen(
            onNavigateBack = back,
            onOpenMatch = { matchId -> navController.navigate(NavRoute.MatchDetail(matchId)) },
            // "Modificar criterios" vuelve al reporte: Mis reportes (L05 agrega el detalle y la edición).
            onModifyCriteria = { navController.navigate(NavRoute.MyReports) },
            onReportLost = { navController.navigateToTab(NavRoute.ReportWizard, NavRoute.Home) }
        )
    }
    // Se abre también desde una notificación: ucbencuentra://match/{matchId} (el guard sigue aplicando).
    guarded<NavRoute.MatchDetail>(
        navController,
        getCurrentUser,
        deepLinks = listOf(navDeepLink<NavRoute.MatchDetail>(basePath = DeepLinks.MATCH_BASE))
    ) { route ->
        MatchDetailScreen(matchId = route.matchId, onNavigateBack = back)
    }
    guarded<NavRoute.Verify>(navController, getCurrentUser) { route ->
        VerifyScreen(matchId = route.matchId, onNavigateBack = back)
    }

    // Staff / Admin
    guarded<NavRoute.Dashboard>(navController, getCurrentUser) {
        DashboardScreen(
            onRegisterFound = { navController.navigate(NavRoute.FoundCapture) },
            onOpenClaims = { navController.navigate(NavRoute.Claims) },
            onOpenClaim = { claimId -> navController.navigate(NavRoute.ClaimDetail(claimId)) },
            onOpenInventory = { navController.navigate(NavRoute.Inventory) }
        )
    }
    // H01: el registro de un hallazgo empieza por las fotos (paso 1) y sigue con el formulario (pasos 2 y 3).
    guarded<NavRoute.FoundCapture>(navController, getCurrentUser) {
        FoundCaptureScreen(onNavigateBack = back, onContinue = { navController.navigate(NavRoute.FoundForm) })
    }
    // Al registrar vuelve a donde se abrió (Inventario); H07 lista el objeto nuevo.
    guarded<NavRoute.FoundForm>(navController, getCurrentUser) {
        // Al registrar se sale de todo el flujo (captura incluida) y se vuelve a donde empezó.
        FoundFormScreen(onNavigateBack = back, onRegistered = { navController.popBackStack<NavRoute.FoundCapture>(inclusive = true) })
    }
    guarded<NavRoute.Inventory>(navController, getCurrentUser) {
        InventoryScreen(onRegisterFound = { navController.navigate(NavRoute.FoundCapture) })
    }
    guarded<NavRoute.Claims>(navController, getCurrentUser) {
        ClaimsScreen(onOpenClaim = { claimId -> navController.navigate(NavRoute.ClaimDetail(claimId)) })
    }
    guarded<NavRoute.ClaimDetail>(navController, getCurrentUser) { route ->
        ClaimDetailScreen(
            claimId = route.claimId,
            onNavigateBack = back,
            onOpenDelivery = { claimId -> navController.navigate(NavRoute.Delivery(claimId)) }
        )
    }
    guarded<NavRoute.Delivery>(navController, getCurrentUser) { route ->
        DeliveryScreen(claimId = route.claimId, onNavigateBack = back)
    }
    guarded<NavRoute.Audit>(navController, getCurrentUser) { AuditScreen(onNavigateBack = back) }

    // Comunes
    // N04: tocar una alerta abre su deep link (match o reclamo), igual que la notificación del sistema.
    guarded<NavRoute.Alerts>(navController, getCurrentUser) { AlertsScreen(onOpenLink = { navController.openDeepLink(it) }) }
    guarded<NavRoute.Profile>(navController, getCurrentUser) {
        ProfileScreen(
            onNavigateToLogin = {
                navController.navigate(NavRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            onOpenDevMenu = { navController.navigate(NavRoute.DevMenu) },
            onOpenUserRoles = { navController.navigate(NavRoute.UserRoles) },
            onNavigateBack = back
        )
    }
    // S03: solo ADMIN (RouteGuard redirige al resto).
    guarded<NavRoute.UserRoles>(navController, getCurrentUser) { UserRolesScreen(onNavigateBack = back) }

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

/** Abre un deep link de la app (`ucbencuentra://...`). Si ninguna ruta lo reconoce, no hace nada en vez de cerrar la app. */
private fun NavHostController.openDeepLink(uri: String) {
    runCatching { navigate(NavUri(uri)) }
}
