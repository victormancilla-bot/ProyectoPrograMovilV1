package bo.edu.ucb.encuentra.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import bo.edu.ucb.encuentra.feature.alerts.presentation.screen.AlertsScreen
import bo.edu.ucb.encuentra.feature.audit.presentation.screen.AuditScreen
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.ClaimDetailScreen
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.ClaimsScreen
import bo.edu.ucb.encuentra.feature.dashboard.presentation.screen.DashboardScreen
import bo.edu.ucb.encuentra.feature.delivery.presentation.screen.DeliveryScreen
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

// F01 deja todas las rutas conectadas a su stub. F05 agrega bottom nav por rol y el guard de sesión.
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = NavRoute.Login) {

        composable<NavRoute.Login> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(NavRoute.DevMenu) {
                        popUpTo<NavRoute.Login> { inclusive = true }
                    }
                },
                onShowToast = { message -> println("Toast: $message") }
            )
        }

        composable<NavRoute.DevMenu> {
            DevMenuScreen(onNavigate = { route -> navController.navigate(route) })
        }

        composable<NavRoute.Home> { HomeScreen(onNavigateBack = back) }
        composable<NavRoute.ReportWizard> { ReportWizardScreen(onNavigateBack = back) }
        composable<NavRoute.MyReports> { MyReportsScreen(onNavigateBack = back) }
        composable<NavRoute.FoundCatalog> { FoundCatalogScreen(onNavigateBack = back) }
        composable<NavRoute.HowItWorks> { HowItWorksScreen(onNavigateBack = back) }
        composable<NavRoute.MatchList> { MatchListScreen(onNavigateBack = back) }
        composable<NavRoute.MatchDetail> { entry ->
            MatchDetailScreen(matchId = entry.toRoute<NavRoute.MatchDetail>().matchId, onNavigateBack = back)
        }
        composable<NavRoute.Verify> { entry ->
            VerifyScreen(matchId = entry.toRoute<NavRoute.Verify>().matchId, onNavigateBack = back)
        }

        composable<NavRoute.FoundCapture> { FoundCaptureScreen(onNavigateBack = back) }
        composable<NavRoute.FoundForm> { FoundFormScreen(onNavigateBack = back) }
        composable<NavRoute.Inventory> { InventoryScreen(onNavigateBack = back) }
        composable<NavRoute.Dashboard> { DashboardScreen(onNavigateBack = back) }
        composable<NavRoute.Claims> { ClaimsScreen(onNavigateBack = back) }
        composable<NavRoute.ClaimDetail> { entry ->
            ClaimDetailScreen(claimId = entry.toRoute<NavRoute.ClaimDetail>().claimId, onNavigateBack = back)
        }
        composable<NavRoute.Delivery> { entry ->
            DeliveryScreen(claimId = entry.toRoute<NavRoute.Delivery>().claimId, onNavigateBack = back)
        }
        composable<NavRoute.Audit> { AuditScreen(onNavigateBack = back) }

        composable<NavRoute.Alerts> { AlertsScreen(onNavigateBack = back) }
        composable<NavRoute.Profile> { ProfileScreen(onNavigateBack = back) }
    }
}
