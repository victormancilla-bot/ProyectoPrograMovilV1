package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable

/** Resultado de pedir la cuenta de Google: el token, que el usuario cerró el selector, o un error. */
sealed class GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Failed(val message: String) : GoogleSignInResult()
}

/**
 * S02 · Pide al sistema la cuenta de Google (expect/actual). Android usa Credential Manager filtrado al dominio
 * `ucb.edu.bo`; iOS queda pendiente (no se compila desde Windows).
 */
@Composable
expect fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit

/** Dominio de Google Workspace de la UCB: el selector solo ofrece cuentas de ese dominio. */
const val UCB_GOOGLE_DOMAIN = "ucb.edu.bo"
