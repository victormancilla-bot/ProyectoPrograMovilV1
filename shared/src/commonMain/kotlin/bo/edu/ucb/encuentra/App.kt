package bo.edu.ucb.encuentra

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.navigation.AppNavHost

@Composable
fun App() {
    MaterialTheme {
        AppNavHost()
    }
}
