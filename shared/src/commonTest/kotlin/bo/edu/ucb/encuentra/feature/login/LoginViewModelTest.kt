package bo.edu.ucb.encuentra.feature.login

import kotlin.test.assertFalse
import bo.edu.ucb.encuentra.core.presentation.GoogleSignInResult
import bo.edu.ucb.encuentra.feature.login.domain.usecase.SignInWithGoogleUseCase
import bo.edu.ucb.encuentra.core.InMemoryUsers
import bo.edu.ucb.encuentra.core.FixedAppConfig
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.vo.Email
import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.domain.usecase.ValidateInstitutionalEmailUseCase
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginEffect
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginEvent
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginStep
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val config = FixedAppConfig()

    private val session = object : SessionRepository {
        var user: UserModel? = null
        override fun getCurrentUser() = user
        override suspend fun setCurrentUser(user: UserModel) { this.user = user }
        override suspend fun clearSession() { user = null }
        override suspend fun restore() = Unit
    }

    private val users = InMemoryUsers()
    private val google = FakeRemoteAuth(mapOf("token-ana" to anaGoogle))
    private val remoteUsers = FakeRemoteUsers()

    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(
            ValidateInstitutionalEmailUseCase(), AuthenticateUseCase(users, session), GetAppConfigUseCase(config),
            SignInWithGoogleUseCase(google, remoteUsers, users, session)
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun nonInstitutionalEmailStaysOnEmailStepWithError() {
        viewModel.handleEvent(LoginEvent.OnEmailChanged("mateo@gmail.com"))
        viewModel.handleEvent(LoginEvent.OnContinue)

        assertEquals(LoginStep.EMAIL, viewModel.state.value.step)
        assertEquals(ValidateInstitutionalEmailUseCase.NOT_INSTITUTIONAL, viewModel.state.value.emailError)
    }

    @Test
    fun validEmailGoesToPasswordAndChangeEmailGoesBack() {
        viewModel.handleEvent(LoginEvent.OnEmailChanged("estudiante@ucb.edu.bo"))
        viewModel.handleEvent(LoginEvent.OnContinue)
        assertEquals(LoginStep.PASSWORD, viewModel.state.value.step)

        viewModel.handleEvent(LoginEvent.OnChangeEmail)
        assertEquals(LoginStep.EMAIL, viewModel.state.value.step)
    }

    @Test
    fun correctPasswordNavigatesHomeAndSavesSession() = runTest(dispatcher) {
        val effect = async { viewModel.effects.first() }
        viewModel.handleEvent(LoginEvent.OnEmailChanged("staff@ucb.edu.bo"))
        viewModel.handleEvent(LoginEvent.OnContinue)
        viewModel.handleEvent(LoginEvent.OnPasswordChanged(AuthenticateUseCase.DEMO_PASSWORD))
        viewModel.handleEvent(LoginEvent.OnContinue)
        advanceUntilIdle()

        assertEquals(LoginEffect.NavigateToHome, effect.await())
        assertEquals("u-ana-staff", session.user?.id)
    }

    @Test
    fun wrongPasswordShowsErrorUnderPassword() = runTest(dispatcher) {
        viewModel.handleEvent(LoginEvent.OnEmailChanged("staff@ucb.edu.bo"))
        viewModel.handleEvent(LoginEvent.OnContinue)
        viewModel.handleEvent(LoginEvent.OnPasswordChanged("otra123"))
        viewModel.handleEvent(LoginEvent.OnContinue)
        advanceUntilIdle()

        assertEquals(AuthenticateUseCase.WRONG_CREDENTIALS, viewModel.state.value.passwordError)
        assertNull(session.user)
    }

    @Test
    fun supportUsesTheContactFromRemoteConfig() = runTest(dispatcher) {
        config.update { it.copy(supportContact = "objetosperdidos@ucb.edu.bo") }

        val effect = async { viewModel.effects.first() }
        viewModel.handleEvent(LoginEvent.OnContactSupport)
        advanceUntilIdle()

        assertEquals(LoginEffect.OpenSupport(LoginViewModel.supportUri("objetosperdidos@ucb.edu.bo")), effect.await())
    }

    @Test
    fun googleSignInNavigatesHomeAndCancelDoesNothing() = runTest(dispatcher) {
        val effect = async { viewModel.effects.first() }
        viewModel.handleEvent(LoginEvent.OnGoogleStarted)
        viewModel.handleEvent(LoginEvent.OnGoogleResult(GoogleSignInResult.Cancelled))
        assertFalse(viewModel.state.value.googleLoading)

        viewModel.handleEvent(LoginEvent.OnGoogleResult(GoogleSignInResult.Failed("Sin cuentas")))
        assertEquals("Sin cuentas", viewModel.state.value.googleError)

        viewModel.handleEvent(LoginEvent.OnGoogleResult(GoogleSignInResult.Token("token-ana")))
        advanceUntilIdle()
        assertEquals(LoginEffect.NavigateToHome, effect.await())
        assertEquals("uid-ana", session.getCurrentUser()?.id)
    }
}
