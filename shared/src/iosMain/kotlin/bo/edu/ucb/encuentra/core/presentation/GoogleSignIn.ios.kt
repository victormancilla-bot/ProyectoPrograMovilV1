package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Pendiente en iOS (GoogleSignIn SDK + Firebase): por ahora se usa el acceso de prueba. */
@Composable
actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { { currentOnResult(GoogleSignInResult.Failed("Inicio de sesión con Google pendiente en iOS.")) } }
}
