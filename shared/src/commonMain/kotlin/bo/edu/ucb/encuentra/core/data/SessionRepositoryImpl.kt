package bo.edu.ucb.encuentra.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/**
 * Sesión persistente: en DataStore solo se guarda el id del usuario; sus datos se leen de Room.
 * En memoria queda una copia para que la navegación (guard) la consulte sin esperar.
 */
class SessionRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val userDao: UserDao
) : SessionRepository {

    private val currentUser = MutableStateFlow<UserModel?>(null)

    override fun getCurrentUser(): UserModel? = currentUser.value

    override suspend fun setCurrentUser(user: UserModel) {
        dataStore.edit { it[USER_ID] = user.id }
        currentUser.value = user
    }

    override suspend fun clearSession() {
        dataStore.edit { it.remove(USER_ID) }
        currentUser.value = null
    }

    override suspend fun restore() {
        val userId = dataStore.data.first()[USER_ID] ?: return
        val user = userDao.getById(userId)?.toModel()
        // Si el usuario ya no existe (se borraron los datos), la sesión guardada no sirve.
        if (user == null) clearSession() else currentUser.value = user
    }

    private companion object {
        val USER_ID = stringPreferencesKey("session_user_id")
    }
}
