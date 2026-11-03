package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Tipo de mensaje: define el color y el ícono del snackbar. */
enum class MessageKind { INFO, SUCCESS, ERROR }

data class UserMessage(val text: String, val kind: MessageKind = MessageKind.INFO)

/**
 * F11 · Mensajes cortos unificados (clase "Toast, SnackBar"): los ViewModels publican aquí y un único
 * `SnackbarHost` del MainScaffold los muestra con el estilo del DS. Así todas las pantallas avisan igual.
 */
interface UserMessages {
    val messages: Flow<UserMessage>

    fun show(text: String, kind: MessageKind = MessageKind.INFO)

    fun success(text: String) = show(text, MessageKind.SUCCESS)

    fun error(text: String) = show(text, MessageKind.ERROR)
}

class UserMessenger : UserMessages {
    private val flow = MutableSharedFlow<UserMessage>(extraBufferCapacity = BUFFER)
    override val messages: Flow<UserMessage> = flow.asSharedFlow()

    override fun show(text: String, kind: MessageKind) {
        flow.tryEmit(UserMessage(text, kind))
    }

    private companion object {
        const val BUFFER = 8
    }
}

/** Lo que recibe el SnackbarHost: el texto y su tipo. */
class AppSnackbarVisuals(val userMessage: UserMessage) : SnackbarVisuals {
    override val message: String get() = userMessage.text
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = if (userMessage.kind == MessageKind.ERROR) SnackbarDuration.Long else SnackbarDuration.Short
}

/** Snackbar con los colores del DS: éxito verde, error rojo, información azul UCB. */
@Composable
fun AppSnackbar(data: SnackbarData) {
    val colors = UcbEncuentraTheme.colors
    val kind = (data.visuals as? AppSnackbarVisuals)?.userMessage?.kind ?: MessageKind.INFO
    val (background, icon) = when (kind) {
        MessageKind.SUCCESS -> colors.success to Icons.Outlined.CheckCircle
        MessageKind.ERROR -> colors.error to Icons.Outlined.ErrorOutline
        MessageKind.INFO -> colors.primary to Icons.Outlined.Info
    }
    Row(
        Modifier.padding(UcbEncuentraTheme.spacing.large).fillMaxWidth()
            .background(background, UcbEncuentraTheme.shapes.medium)
            .padding(horizontal = UcbEncuentraTheme.spacing.large, vertical = UcbEncuentraTheme.spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
        AppBody(data.visuals.message, color = colors.onPrimary)
    }
}

/** Banner "sin conexión" (F11): la app sigue funcionando con lo guardado en el teléfono. */
@Composable
fun OfflineBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = UcbEncuentraTheme.colors
    Row(
        modifier.fillMaxWidth().background(colors.surfaceContainer)
            .padding(horizontal = UcbEncuentraTheme.spacing.large, vertical = UcbEncuentraTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = colors.error, modifier = Modifier.size(20.dp))
        AppLabel(
            "Sin conexión. Puedes seguir usando lo guardado en el teléfono.",
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { AppLabel("Reintentar", color = colors.primary) }
    }
}
