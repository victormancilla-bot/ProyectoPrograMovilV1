package bo.edu.ucb.encuentra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase
import bo.edu.ucb.encuentra.core.data.local.seed.DatabaseSeeder
import bo.edu.ucb.encuentra.navigation.AppNavHost
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.koinInject

@Composable
fun App(
    seeder: DatabaseSeeder = koinInject(),
    restoreSession: RestoreSessionUseCase = koinInject()
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 1) Primera ejecución: datos de prueba 2026 (F06). 2) Sesión guardada (S01), que necesita los usuarios.
        seeder.seedIfEmpty()
        restoreSession()
        ready = true
    }

    UcbEncuentraTheme {
        // La navegación arranca recién con la sesión restaurada: así el guard decide Login u home del rol.
        if (ready) AppNavHost() else AppSurface { LoadingState() }
    }
}
