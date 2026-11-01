package bo.edu.ucb.encuentra.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val devRoutes: List<Pair<String, NavRoute>> = listOf(
    "Design System (E)" to NavRoute.DesignSystemDemo,
    "Home (C)" to NavRoute.Home,
    "Reportar (C)" to NavRoute.ReportWizard,
    "Mis reportes (C)" to NavRoute.MyReports,
    "Objetos encontrados (C)" to NavRoute.FoundCatalog,
    "Cómo funciona (C)" to NavRoute.HowItWorks,
    "Alertas (C)" to NavRoute.Alerts,
    "Captura de foto (D)" to NavRoute.FoundCapture,
    "Registrar objeto (D)" to NavRoute.FoundForm,
    "Inventario (D)" to NavRoute.Inventory,
    "Coincidencias (E)" to NavRoute.MatchList,
    "Detalle de match (E)" to NavRoute.MatchDetail(matchId = "demo"),
    "Perfil (E)" to NavRoute.Profile,
    "Verificación (B)" to NavRoute.Verify(matchId = "demo"),
    "Entrega (B)" to NavRoute.Delivery(claimId = "demo"),
    "Auditoría (B)" to NavRoute.Audit,
    "Dashboard (A)" to NavRoute.Dashboard,
    "Reclamos (A)" to NavRoute.Claims,
    "Detalle de reclamo (A)" to NavRoute.ClaimDetail(claimId = "demo")
)

/** Menú temporal para abrir cualquier stub. Se elimina cuando F05 agregue la navegación por rol. */
@Composable
fun DevMenuScreen(onNavigate: (NavRoute) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Menú de desarrollo", style = MaterialTheme.typography.headlineSmall)
        }
        items(devRoutes) { (label, route) ->
            OutlinedButton(
                onClick = { onNavigate(route) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(label)
            }
        }
    }
}
