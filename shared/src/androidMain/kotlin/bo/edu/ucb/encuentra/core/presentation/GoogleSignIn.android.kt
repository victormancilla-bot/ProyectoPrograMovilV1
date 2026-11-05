package bo.edu.ucb.encuentra.core.presentation

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Android: Credential Manager con "Iniciar sesión con Google". El `serverClientId` es el cliente web de Firebase
 * (`default_web_client_id`, lo genera el plugin de google-services desde `google-services.json`).
 */
@Composable
actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(context) {
        {
            scope.launch {
                currentOnResult(requestGoogleToken(context))
            }
        }
    }
}

private suspend fun requestGoogleToken(context: Context): GoogleSignInResult {
    val clientId = context.webClientId()
        ?: return GoogleSignInResult.Failed("Falta configurar Firebase (google-services.json) en esta app.")
    val option = GetSignInWithGoogleOption.Builder(clientId)
        .setHostedDomainFilter(UCB_GOOGLE_DOMAIN)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInResult.Failed("La cuenta elegida no es de Google.")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleSignInResult.Failed("No hay cuentas @$UCB_GOOGLE_DOMAIN en este teléfono. Agrega tu cuenta de la UCB en Ajustes > Cuentas.")
    } catch (e: Exception) {
        Log.w(GOOGLE_TAG, "Credential Manager falló", e)
        GoogleSignInResult.Failed("No se pudo abrir el inicio de sesión con Google.")
    }
}

/** `default_web_client_id` del módulo de la app (el shared no tiene su R). */
private fun Context.webClientId(): String? {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (id == 0) null else getString(id)
}

private const val GOOGLE_TAG = "UcbGoogle"
