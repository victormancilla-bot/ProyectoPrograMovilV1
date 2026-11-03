package bo.edu.ucb.encuentra.feature.profile.presentation.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import bo.edu.ucb.encuentra.core.presentation.FeatureStub
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEffect
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEvent
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onOpenDevMenu: () -> Unit,
    onOpenUserRoles: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    FeatureStub(
        title = "Perfil y ajustes",
        owner = "Integrante E",
        card = "S07",
        actions = {
            // S03: solo el ADMIN entra al panel de roles.
            if (state.canManageRoles) Button(onClick = onOpenUserRoles) { Text("Roles y permisos") }
            Button(onClick = { viewModel.handleEvent(ProfileEvent.OnLogout) }) { Text("Cerrar sesión") }
            TextButton(onClick = onOpenDevMenu) { Text("Menú de desarrollo") }
        },
        onNavigateBack = onNavigateBack
    )
}
