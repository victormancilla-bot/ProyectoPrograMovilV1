package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

sealed class ProfileEffect {
    data object NavigateToLogin : ProfileEffect()
}
