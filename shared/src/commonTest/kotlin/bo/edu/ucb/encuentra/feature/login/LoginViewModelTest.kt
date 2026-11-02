package bo.edu.ucb.encuentra.feature.login

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
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

    private val session = object : SessionRepository {
        var user: UserModel? = null
        override fun getCurrentUser() = user
        override suspend fun setCurrentUser(user: UserModel) { this.user = user }
        override suspend fun clearSession() { user = null }
        override suspend fun restore() = Unit
    }

    private val users = object : UserRepository {
        override suspend fun findByEmail(email: Email) =
            SeedData.users.firstOrNull { it.email.value == email.value.trim().lowercase() }
    }

    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(ValidateInstitutionalEmailUseCase(), AuthenticateUseCase(users, session))
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
}
