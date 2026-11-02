package bo.edu.ucb.encuentra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import bo.edu.ucb.encuentra.core.data.local.seed.DatabaseSeeder
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveAppConfigUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.RefreshAppConfigUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase
import bo.edu.ucb.encuentra.core.presentation.MaintenanceScreen
import bo.edu.ucb.encuentra.navigation.AppNavHost
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App(
    seeder: DatabaseSeeder = koinInject(),
    restoreSession: RestoreSessionUseCase = koinInject(),
    refreshConfig: RefreshAppConfigUseCase = koinInject(),
    observeConfig: ObserveAppConfigUseCase = koinInject()
) {
    var ready by remember { mutableStateOf(false) }
    var retrying by remember { mutableStateOf(false) }
    val config by observeConfig().collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // F08: la configuración remota se pide al abrir la app, en paralelo. Sin internet o sin Firebase se sigue
        // con los valores por defecto (el error ya viene como Result, no rompe el arranque).
        launch { refreshConfig() }
        // 1) Primera ejecución: datos de prueba 2026 (F06). 2) Sesión guardada (S01), que necesita los usuarios.
        seeder.seedIfEmpty()
        restoreSession()
        ready = true
    }

    UcbEncuentraTheme {
        when {
            // La oficina puede apagar la app desde la consola (maintenance_mode) sin publicar una versión.
            config.maintenanceMode -> MaintenanceScreen(
                supportContact = config.supportContact,
                retrying = retrying,
                onRetry = {
                    scope.launch {
                        retrying = true
                        refreshConfig(force = true)
                        retrying = false
                    }
                }
            )
            // La navegación arranca recién con la sesión restaurada: así el guard decide Login u home del rol.
            ready -> AppNavHost()
            else -> AppSurface { LoadingState() }
        }
    }
}
