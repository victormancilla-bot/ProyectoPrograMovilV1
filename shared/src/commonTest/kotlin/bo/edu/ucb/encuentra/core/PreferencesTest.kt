package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.domain.model.AppLanguage
import bo.edu.ucb.encuentra.core.domain.model.BolivianPhone
import bo.edu.ucb.encuentra.core.domain.model.DevicePreferences
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.ThemeMode
import bo.edu.ucb.encuentra.core.domain.model.UserPreferences
import bo.edu.ucb.encuentra.core.domain.repository.NoRemoteAuth
import bo.edu.ucb.encuentra.core.domain.repository.PreferencesRepository
import bo.edu.ucb.encuentra.core.domain.repository.PushTokenRegistry
import bo.edu.ucb.encuentra.core.domain.usecase.GetContactPhoneUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveDevicePreferencesUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveUserPreferencesUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SaveContactPhoneUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetLanguageUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetNotificationPreferenceUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.SetThemeUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.feature.foundform.FakeSession
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.foundform.demoUser
import bo.edu.ucb.encuentra.feature.myreports.mateo
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.LogoutUseCase
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEffect
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileEvent
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DataStore en memoria. */
internal class InMemoryPreferences : PreferencesRepository {
    val device = MutableStateFlow(DevicePreferences())
    val users = MutableStateFlow<Map<String, UserPreferences>>(emptyMap())

    override fun observeDevice() = device
    override suspend fun setTheme(theme: ThemeMode) = device.update { it.copy(theme = theme) }
    override suspend fun setLanguage(language: AppLanguage) = device.update { it.copy(language = language) }
    override suspend fun markNotificationsRationaleShown() = device.update { it.copy(notificationsRationaleShown = true) }
    override fun observeUser(userId: String) = users.map { it[userId] ?: UserPreferences() }
    override suspend fun setNotification(userId: String, type: NotificationType, enabled: Boolean) = users.update { map ->
        val prefs = map[userId] ?: UserPreferences()
        val disabled = if (enabled) prefs.disabledNotifications - type else prefs.disabledNotifications + type
        map + (userId to prefs.copy(disabledNotifications = disabled))
    }
    override suspend fun setContactPhone(userId: String, phone: String) = users.update { map ->
        map + (userId to (map[userId] ?: UserPreferences()).copy(contactPhone = BolivianPhone.normalize(phone)))
    }
}

private class RecordingPushTokens : PushTokenRegistry {
    var unregistered = 0
    override suspend fun unregister() {
        unregistered++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesTest {

    private val dispatcher = StandardTestDispatcher()
    private val prefs = InMemoryPreferences()
    private val messages = RecordingMessages()
    private val session = FakeSession(mateo)
    private val user = GetCurrentUserUseCase(session)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun bolivianPhoneRule() {
        assertTrue(BolivianPhone.isValid("71234567"))
        assertTrue(BolivianPhone.isValid("+591 712-34567"))
        assertFalse(BolivianPhone.isValid("1234567"))
        assertFalse(BolivianPhone.isValid("81234567"))
    }

    @Test
    fun contactPhoneIsSavedPerUserAndPrefillsTheReport() = runTest(dispatcher) {
        val save = SaveContactPhoneUseCase(prefs, user)
        assertEquals("71234567", save("712 34567").getOrThrow())
        assertEquals(SaveContactPhoneUseCase.INVALID, save("123").exceptionOrNull()?.message)
        assertEquals("71234567", GetContactPhoneUseCase(prefs, user)())

        // Otro usuario en el mismo teléfono no ve el número de Mateo.
        session.setCurrentUser(demoUser(Role.ESTUDIANTE).copy(id = "u-ana-gomez"))
        assertEquals("", GetContactPhoneUseCase(prefs, user)())

        // Vacío lo borra.
        session.setCurrentUser(mateo)
        save("").getOrThrow()
        assertEquals("", GetContactPhoneUseCase(prefs, user)())
    }

    @Test
    fun disabledNotificationTypesAreSavedButNotShown() = runTest(dispatcher) {
        SetNotificationPreferenceUseCase(prefs, user)(NotificationType.NEW_MATCH, enabled = false)
        val notifier = RecordingNotifier()
        val inbox = InMemoryNotifications()
        val center = NotificationCenter(inbox, notifier, user, prefs)

        fun alert(id: String, type: NotificationType) =
            AppNotification(ItemId(id), mateo.id, type, id, "", null, false, FixedClock.now())
        center.publish(listOf(alert("match", NotificationType.NEW_MATCH), alert("claim", NotificationType.CLAIM_UPDATE)))

        assertEquals(listOf("claim"), notifier.shown.map { it.id.value })
        assertEquals(2, inbox.saved.size)
    }

    @Test
    fun logoutUnregistersThePushTokenAndClearsTheSession() = runTest(dispatcher) {
        val tokens = RecordingPushTokens()
        LogoutUseCase(session, tokens, NoRemoteAuth)()
        assertEquals(1, tokens.unregistered)
        assertNull(session.getCurrentUser())
    }

    private fun profile() = ProfileViewModel(
        LogoutUseCase(session, RecordingPushTokens(), NoRemoteAuth),
        user,
        ObserveDevicePreferencesUseCase(prefs),
        ObserveUserPreferencesUseCase(prefs, user),
        SetThemeUseCase(prefs),
        SetLanguageUseCase(prefs),
        SetNotificationPreferenceUseCase(prefs, user),
        SaveContactPhoneUseCase(prefs, user),
        messages
    )

    @Test
    fun profileShowsTheUserAndChangesPreferences() = runTest(dispatcher) {
        val vm = profile()
        advanceUntilIdle()
        assertEquals(mateo, vm.state.value.user)
        assertEquals(listOf(NotificationType.NEW_MATCH, NotificationType.CLAIM_UPDATE), vm.state.value.notificationTypes)
        assertFalse(vm.state.value.canManageRoles)

        vm.handleEvent(ProfileEvent.OnThemeSelected(ThemeMode.OSCURO))
        vm.handleEvent(ProfileEvent.OnLanguageSelected(AppLanguage.EN))
        vm.handleEvent(ProfileEvent.OnNotificationToggled(NotificationType.CLAIM_UPDATE, false))
        advanceUntilIdle()
        assertEquals(DevicePreferences(ThemeMode.OSCURO, AppLanguage.EN), vm.state.value.device)
        assertFalse(vm.state.value.prefs.notifies(NotificationType.CLAIM_UPDATE))

        vm.handleEvent(ProfileEvent.OnPhoneChanged("7123"))
        vm.handleEvent(ProfileEvent.OnSavePhone)
        advanceUntilIdle()
        assertEquals(SaveContactPhoneUseCase.INVALID, vm.state.value.phoneError)

        vm.handleEvent(ProfileEvent.OnPhoneChanged("71234567"))
        assertTrue(vm.state.value.phoneChanged)
        vm.handleEvent(ProfileEvent.OnSavePhone)
        advanceUntilIdle()
        assertEquals("71234567", vm.state.value.prefs.contactPhone)
        assertFalse(vm.state.value.phoneChanged)
        assertEquals("WhatsApp guardado: se usará en tus reportes.", messages.last?.text)
    }

    @Test
    fun staffOnlyChoosesRetentionAndLogoutAsksFirst() = runTest(dispatcher) {
        session.setCurrentUser(demoUser(Role.STAFF))
        val vm = profile()
        val effect = async { vm.effects.first() }
        advanceUntilIdle()
        assertEquals(listOf(NotificationType.RETENTION), vm.state.value.notificationTypes)

        vm.handleEvent(ProfileEvent.OnRequestLogout)
        assertTrue(vm.state.value.confirmLogout)
        vm.handleEvent(ProfileEvent.OnLogout)
        advanceUntilIdle()
        assertEquals(ProfileEffect.NavigateToLogin, effect.await())
        assertNull(session.getCurrentUser())
    }
}
