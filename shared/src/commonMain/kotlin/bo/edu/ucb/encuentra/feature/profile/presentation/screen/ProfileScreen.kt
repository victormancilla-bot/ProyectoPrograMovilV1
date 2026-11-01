package bo.edu.ucb.encuentra.feature.profile.presentation.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onNavigateBack: () -> Unit
) {
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
            Button(onClick = { viewModel.handleEvent(ProfileEvent.OnLogout) }) { Text("Cerrar sesión") }
            TextButton(onClick = onOpenDevMenu) { Text("Menú de desarrollo") }
        },
        onNavigateBack = onNavigateBack
    )
}
