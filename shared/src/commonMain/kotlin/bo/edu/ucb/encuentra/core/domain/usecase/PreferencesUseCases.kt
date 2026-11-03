package bo.edu.ucb.encuentra.core.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.BolivianPhone
import bo.edu.ucb.encuentra.core.domain.model.DevicePreferences
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode
import bo.edu.ucb.encuentra.core.domain.model.UserPreferences
import bo.edu.ucb.encuentra.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/** Tema e idioma del teléfono: App.kt los aplica en vivo (S07). */
class ObserveDevicePreferencesUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<DevicePreferences> = repository.observeDevice()
}

class SetThemeUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(theme: ThemeMode) = repository.setTheme(theme)
}

class SetLanguageUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

/** Preferencias del usuario con sesión (sin sesión, las de por defecto). */
class ObserveUserPreferencesUseCase(
    private val repository: PreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<UserPreferences> =
        getCurrentUser()?.let { repository.observeUser(it.id) } ?: flowOf(UserPreferences())
}

class SetNotificationPreferenceUseCase(
    private val repository: PreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(type: NotificationType, enabled: Boolean) {
        getCurrentUser()?.let { repository.setNotification(it.id, type, enabled) }
    }
}

/** Guarda el WhatsApp de contacto (S07). Vacío lo borra; si no es un celular boliviano válido, falla. */
class SaveContactPhoneUseCase(
    private val repository: PreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(phone: String): Result<String> {
        val user = getCurrentUser() ?: return Result.failure(IllegalStateException(NO_SESSION))
        val normalized = BolivianPhone.normalize(phone)
        if (normalized.isNotEmpty() && !BolivianPhone.isValid(normalized)) {
            return Result.failure(IllegalArgumentException(INVALID))
        }
        repository.setContactPhone(user.id, normalized)
        return Result.success(normalized)
    }

    companion object {
        const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
        const val INVALID = "Ingresa un celular válido de 8 dígitos (p. ej. 71234567)"
    }
}

/** El WhatsApp guardado, para prellenar el contacto del wizard de reporte (L03). */
class GetContactPhoneUseCase(
    private val repository: PreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(): String =
        getCurrentUser()?.let { repository.observeUser(it.id).first().contactPhone }.orEmpty()
}
