package bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode

sealed class ProfileEvent {
    data class OnThemeSelected(val theme: ThemeMode) : ProfileEvent()
    data class OnLanguageSelected(val language: AppLanguage) : ProfileEvent()
    data class OnNotificationToggled(val type: NotificationType, val enabled: Boolean) : ProfileEvent()
    data class OnPhoneChanged(val value: String) : ProfileEvent()
    data object OnSavePhone : ProfileEvent()
    data object OnRequestLogout : ProfileEvent()
    data object OnDismissLogout : ProfileEvent()
    data object OnLogout : ProfileEvent()
}
