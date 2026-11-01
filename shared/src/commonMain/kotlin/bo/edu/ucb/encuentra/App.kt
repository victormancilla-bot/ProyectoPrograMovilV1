package bo.edu.ucb.encuentra

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.navigation.AppNavHost
import edu.ucb.ds.theme.UcbEncuentraTheme

@Composable
fun App() {
    UcbEncuentraTheme {
        AppNavHost()
    }
}
