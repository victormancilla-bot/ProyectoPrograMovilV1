package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

sealed class ProfileEvent {
    data object OnLogout : ProfileEvent()
}
