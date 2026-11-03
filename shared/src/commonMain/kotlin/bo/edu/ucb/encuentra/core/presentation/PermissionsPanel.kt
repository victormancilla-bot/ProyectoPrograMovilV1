package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.theme.UcbEncuentraTheme

/**
 * F11 · Onboarding de permisos dentro del Perfil: para qué sirve cada uno, si está activado y cómo activarlo
 * (con la justificación antes de pedirlo, o Ajustes si ya se negó para siempre). Se vuelve a leer al regresar
 * de Ajustes.
 */
@Composable
fun PermissionsPanel(permissions: List<AppPermission>) {
    // Sube cada vez que la pantalla vuelve a primer plano (p. ej. desde Ajustes) para releer los permisos.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
        permissions.forEach { PermissionRow(it, refresh) { refresh++ } }
    }
}

@Composable
private fun PermissionRow(permission: AppPermission, refresh: Int, onChanged: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    var showRationale by remember { mutableStateOf(false) }
    val requester = rememberPermissionRequester(permission) { onChanged() }
    // `refresh` hace que se vuelva a consultar el estado real del sistema.
    val granted = remember(refresh, requester) { requester.isGranted() }

    Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap), verticalAlignment = Alignment.CenterVertically) {
        Icon(permission.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            AppTitle(permission.title)
            AppLabel(permission.why, color = colors.onSurfaceVariant)
        }
        if (granted) {
            StatusChip(text = "Activado", tone = StatusTone.Success)
        } else {
            TextButton(onClick = { showRationale = true }) { AppLabel("Activar", color = colors.primary) }
        }
    }

    if (showRationale) {
        val permanentlyDenied = requester.isPermanentlyDenied()
        PermissionRationaleDialog(
            permission = permission,
            permanentlyDenied = permanentlyDenied,
            onContinue = {
                showRationale = false
                if (permanentlyDenied) requester.openSettings() else requester.request()
            },
            onDismiss = { showRationale = false }
        )
    }
}
