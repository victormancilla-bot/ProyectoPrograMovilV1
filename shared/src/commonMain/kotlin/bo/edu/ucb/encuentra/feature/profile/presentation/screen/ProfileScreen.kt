package bo.edu.ucb.encuentra.feature.profile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.label
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEffect
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEvent
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileState
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileViewModel
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.cards.AppCard
import edu.ucb.ds.components.chips.AppChipGroup
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.selection.CheckRow
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * S07 · Perfil y Ajustes (pestaña Perfil; no está en el PDF, se arma con el DS como el UserState de ucbp1):
 * datos del usuario, WhatsApp de contacto, tema, idioma, avisos y cerrar sesión.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onOpenDevMenu: () -> Unit,
    onOpenUserRoles: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    ProfileContent(state, viewModel::handleEvent, onOpenDevMenu, onOpenUserRoles)
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onOpenDevMenu: () -> Unit,
    onOpenUserRoles: () -> Unit
) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(title = "Perfil", subtitle = "Datos y ajustes")
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                state.user?.let { UserCard(it) }
                ContactSection(state, onEvent)
                AppearanceSection(state, onEvent)
                NotificationsSection(state, onEvent)
                if (state.canManageRoles) {
                    Section("Administración") {
                        PrimaryButton(
                            text = "Roles y permisos",
                            onClick = onOpenUserRoles,
                            leadingIcon = Icons.Outlined.AdminPanelSettings,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                SecondaryButton(
                    text = "Cerrar sesión",
                    onClick = { onEvent(ProfileEvent.OnRequestLogout) },
                    enabled = !state.loggingOut,
                    color = UcbEncuentraTheme.colors.error,
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                // Solo mientras quedan pantallas de desarrollo (se quita antes del entregable final).
                TextButton(onClick = onOpenDevMenu, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    AppLabel("Menú de desarrollo", color = UcbEncuentraTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
    if (state.confirmLogout) {
        AlertDialog(
            onDismissRequest = { onEvent(ProfileEvent.OnDismissLogout) },
            title = { AppTitle("¿Cerrar sesión?") },
            text = { AppBody("Dejarás de recibir los avisos de tu cuenta en este teléfono hasta que vuelvas a ingresar.") },
            confirmButton = {
                TextButton(onClick = { onEvent(ProfileEvent.OnLogout) }) {
                    AppLabel("Cerrar sesión", color = UcbEncuentraTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProfileEvent.OnDismissLogout) }) {
                    AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        AppOverline(title)
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap), content = content)
        }
    }
}

@Composable
private fun UserCard(user: UserModel) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    val initials = user.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(64.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                AppTitle(initials, color = colors.onPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                AppTitle(user.fullName)
                AppLabel(user.email.value, color = colors.onSurfaceVariant)
                AppLabel(listOfNotNull(user.faculty, user.campus.label).joinToString(" • "), color = colors.onSurfaceVariant)
                StatusChip(
                    text = user.role.label,
                    tone = when (user.role) {
                        Role.ESTUDIANTE -> StatusTone.Neutral
                        Role.STAFF -> StatusTone.Info
                        Role.ADMIN -> StatusTone.Primary
                    }
                )
            }
        }
    }
}

@Composable
private fun ContactSection(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    Section("Contacto") {
        AppBody("Tu WhatsApp se prellena en tus reportes para que Objetos Perdidos te contacte más rápido. Es opcional.")
        AppTextField(
            value = state.phone,
            onValueChange = { onEvent(ProfileEvent.OnPhoneChanged(it)) },
            placeholder = "WhatsApp, p. ej. 71234567",
            leadingIcon = Icons.Outlined.Phone,
            errorMessage = state.phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        PrimaryButton(
            text = "Guardar WhatsApp",
            onClick = { onEvent(ProfileEvent.OnSavePhone) },
            enabled = state.phoneChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppearanceSection(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    Section("Apariencia e idioma") {
        AppLabel("Tema")
        AppChipGroup(
            options = ThemeMode.entries.map { it.label },
            selected = state.device.theme.label,
            onSelect = { label -> ThemeMode.entries.firstOrNull { it.label == label }?.let { onEvent(ProfileEvent.OnThemeSelected(it)) } }
        )
        AppLabel("Idioma")
        AppChipGroup(
            options = AppLanguage.entries.map { it.label },
            selected = state.device.language.label,
            onSelect = { label -> AppLanguage.entries.firstOrNull { it.label == label }?.let { onEvent(ProfileEvent.OnLanguageSelected(it)) } }
        )
        if (state.device.language != AppLanguage.ES) {
            AppLabel("La traducción al inglés está en preparación: por ahora los textos siguen en español.", color = UcbEncuentraTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotificationsSection(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    Section("Notificaciones") {
        AppBody("Elige qué avisos ver en la barra del teléfono. Igual quedan en tu bandeja de Alertas.")
        state.notificationTypes.forEach { type ->
            CheckRow(
                text = type.description(),
                checked = state.prefs.notifies(type),
                onCheckedChange = { onEvent(ProfileEvent.OnNotificationToggled(type, it)) }
            )
        }
    }
}

private fun NotificationType.description(): String = when (this) {
    NotificationType.NEW_MATCH -> "Coincidencias: apareció algo parecido a lo que reportaste"
    NotificationType.CLAIM_UPDATE -> "Reclamos: aprobado, rechazado o fecha de entrega"
    NotificationType.RETENTION -> "Retención: objetos vencidos o por vencer"
    NotificationType.GENERAL -> "Avisos generales"
}
