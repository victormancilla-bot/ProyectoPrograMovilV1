package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.DevicePreferences
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode
import bo.edu.ucb.encuentra.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Preferencias de S07 (DataStore, la misma de la sesión). */
interface PreferencesRepository {
    fun observeDevice(): Flow<DevicePreferences>

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    fun observeUser(userId: String): Flow<UserPreferences>

    suspend fun setNotification(userId: String, type: NotificationType, enabled: Boolean)

    suspend fun setContactPhone(userId: String, phone: String)
}

/**
 * Hook del token push (N02/FCM): al cerrar sesión se borra para que el teléfono deje de recibir los avisos de
 * esa persona. Hasta N02 no hay token, así que no hace nada.
 */
interface PushTokenRegistry {
    suspend fun unregister()
}

object NoPushToken : PushTokenRegistry {
    override suspend fun unregister() = Unit
}
