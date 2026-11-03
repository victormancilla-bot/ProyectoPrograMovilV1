package bo.edu.ucb.encuentra.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.BolivianPhone
import bo.edu.ucb.encuentra.core.domain.model.DevicePreferences
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode
import bo.edu.ucb.encuentra.core.domain.model.UserPreferences
import bo.edu.ucb.encuentra.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Preferencias en la DataStore de la sesión (S01). Claves del teléfono: `theme_mode`, `app_language`; por
 * usuario: `notify_<tipo>_<userId>` y `contact_phone_<userId>`.
 */
class DataStorePreferencesRepository(private val dataStore: DataStore<Preferences>) : PreferencesRepository {

    override fun observeDevice(): Flow<DevicePreferences> = dataStore.data.map { prefs ->
        DevicePreferences(
            theme = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SISTEMA,
            language = prefs[LANGUAGE]?.let { tag -> AppLanguage.entries.firstOrNull { it.tag == tag } } ?: AppLanguage.ES,
            notificationsRationaleShown = prefs[RATIONALE_SHOWN] == true
        )
    }.distinctUntilChanged()

    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE] = language.tag }
    }

    override suspend fun markNotificationsRationaleShown() {
        dataStore.edit { it[RATIONALE_SHOWN] = true }
    }

    override fun observeUser(userId: String): Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            disabledNotifications = NotificationType.entries.filter { prefs[notifyKey(userId, it)] == false }.toSet(),
            contactPhone = prefs[phoneKey(userId)].orEmpty()
        )
    }.distinctUntilChanged()

    override suspend fun setNotification(userId: String, type: NotificationType, enabled: Boolean) {
        dataStore.edit { it[notifyKey(userId, type)] = enabled }
    }

    override suspend fun setContactPhone(userId: String, phone: String) {
        val normalized = BolivianPhone.normalize(phone)
        dataStore.edit { if (normalized.isEmpty()) it.remove(phoneKey(userId)) else it[phoneKey(userId)] = normalized }
    }

    private fun notifyKey(userId: String, type: NotificationType) = booleanPreferencesKey("notify_${type.name.lowercase()}_$userId")

    private fun phoneKey(userId: String) = stringPreferencesKey("contact_phone_$userId")

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val RATIONALE_SHOWN = booleanPreferencesKey("notifications_rationale_shown")
    }
}
