package bo.edu.ucb.encuentra

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import bo.edu.ucb.encuentra.navigation.ExternalUriHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        Log.d("CONFIG_TEST", "BASE_URL: ${BuildConfig.BASE_URL}")
        Log.d("CONFIG_TEST", "APP_NAME: ${BuildConfig.APP_NAME}")

        setContent {
            App()
        }
    }

    // Deep link (ucbencuentra://match/...) con la app ya abierta: se lo pasamos a la navegación.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { ExternalUriHandler.onNewUri(it.toString()) }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}