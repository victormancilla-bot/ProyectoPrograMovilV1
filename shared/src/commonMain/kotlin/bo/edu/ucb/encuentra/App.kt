package bo.edu.ucb.encuentra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import bo.edu.ucb.encuentra.core.data.local.seed.DatabaseSeeder
import bo.edu.ucb.encuentra.navigation.AppNavHost
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.koinInject

@Composable
fun App(seeder: DatabaseSeeder = koinInject()) {
    // Primera ejecución: carga los datos de prueba 2026 (F06). No hace nada si la base ya tiene datos.
    LaunchedEffect(Unit) { seeder.seedIfEmpty() }

    UcbEncuentraTheme {
        AppNavHost()
    }
}
