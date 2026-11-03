package bo.edu.ucb.encuentra.feature.profile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.label
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.RoleFilter
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesEffect
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesEvent
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesState
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.UserRolesViewModel
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
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** S03 · Roles y permisos (solo ADMIN): asignar staff por correo y quitarlo. No hay mockup en Figma: usa el DS. */
@Composable
fun UserRolesScreen(
    viewModel: UserRolesViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UserRolesEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    UserRolesContent(state, viewModel::handleEvent)
}

@Composable
private fun UserRolesContent(state: UserRolesState, onEvent: (UserRolesEvent) -> Unit) {
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = "Roles y permisos",
                subtitle = "Administración",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = { onEvent(UserRolesEvent.OnBack) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> LoadingState(message = "Cargando cuentas...")
                    state.error != null -> ErrorState(message = state.error, onRetry = null)
                    else -> UserRolesBody(state, onEvent)
                }
            }
        }
    }
    state.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = { onEvent(UserRolesEvent.OnDismissChange) },
            title = { AppTitle("¿Quitar staff?") },
            text = {
                AppBody(
                    "${pending.user.fullName} pasará a ${pending.newRole.label} y ya no verá el Panel, el Inventario " +
                        "ni los Reclamos. Se registra en la auditoría."
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(UserRolesEvent.OnConfirmChange) }) {
                    AppLabel("Quitar staff", color = UcbEncuentraTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(UserRolesEvent.OnDismissChange) }) {
                    AppLabel("Cancelar", color = UcbEncuentraTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun UserRolesBody(state: UserRolesState, onEvent: (UserRolesEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        item { AssignStaffCard(state, onEvent) }
        item {
            AppOverline("Cuentas", modifier = Modifier.padding(top = spacing.medium))
        }
        item {
            AppChipGroup(
                options = RoleFilter.entries.map { filterLabel(it, state) },
                selected = filterLabel(state.filter, state),
                onSelect = { label ->
                    RoleFilter.entries.firstOrNull { filterLabel(it, state) == label }
                        ?.let { onEvent(UserRolesEvent.OnFilterChanged(it)) }
                }
            )
        }
        if (state.visibleUsers.isEmpty()) {
            item { EmptyState(title = "Sin cuentas", message = "No hay cuentas con ese rol.") }
        }
        items(state.visibleUsers, key = { it.id }) { user -> UserRow(user, state, onEvent) }
    }
}

private fun filterLabel(filter: RoleFilter, state: UserRolesState): String =
    filter.role?.let { "${filter.label} (${state.countOf(it)})" } ?: "${filter.label} (${state.users.size})"

@Composable
private fun AssignStaffCard(state: UserRolesState, onEvent: (UserRolesEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            AppTitle("Asignar staff por correo")
            AppBody(
                "La persona debe haber ingresado una vez a la app. Nadie puede asignarse staff a sí mismo.",
                color = UcbEncuentraTheme.colors.onSurfaceVariant
            )
            AppTextField(
                value = state.email,
                onValueChange = { onEvent(UserRolesEvent.OnEmailChanged(it)) },
                placeholder = "nombre.apellido@ucb.edu.bo",
                leadingIcon = Icons.Outlined.Email,
                errorMessage = state.emailError,
                enabled = !state.working,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
            )
            PrimaryButton(
                text = "Asignar staff",
                onClick = { onEvent(UserRolesEvent.OnAssignStaff) },
                enabled = !state.working,
                leadingIcon = Icons.Outlined.PersonAdd,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UserRow(user: UserModel, state: UserRolesState, onEvent: (UserRolesEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    val colors = UcbEncuentraTheme.colors
    val isMe = user.id == state.currentUserId
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.large), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                Initials(user)
                Column(Modifier.weight(1f)) {
                    AppTitle(if (isMe) "${user.fullName} (tú)" else user.fullName)
                    AppLabel(user.email.value, color = colors.onSurfaceVariant)
                    user.faculty?.let { AppLabel(it, color = colors.onSurfaceVariant) }
                }
                StatusChip(text = user.role.label, tone = user.role.tone())
            }
            when {
                state.canRemoveStaff(user) -> SecondaryButton(
                    text = "Quitar staff",
                    onClick = { onEvent(UserRolesEvent.OnRequestChange(user, Role.ESTUDIANTE)) },
                    enabled = !state.working,
                    color = colors.error,
                    modifier = Modifier.fillMaxWidth()
                )
                state.canMakeStaff(user) -> SecondaryButton(
                    text = "Hacer staff",
                    onClick = { onEvent(UserRolesEvent.OnRequestChange(user, Role.STAFF)) },
                    enabled = !state.working,
                    modifier = Modifier.fillMaxWidth()
                )
                isMe || user.role == Role.ADMIN -> AppLabel(
                    if (isMe) "No puedes cambiar tu propio rol." else "Las cuentas de administrador no se cambian aquí.",
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Initials(user: UserModel) {
    val colors = UcbEncuentraTheme.colors
    val initials = user.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(44.dp).background(colors.secondary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AppLabel(initials, color = colors.primary, textAlign = TextAlign.Center)
    }
}

private fun Role.tone(): StatusTone = when (this) {
    Role.ESTUDIANTE -> StatusTone.Neutral
    Role.STAFF -> StatusTone.Info
    Role.ADMIN -> StatusTone.Primary
}
