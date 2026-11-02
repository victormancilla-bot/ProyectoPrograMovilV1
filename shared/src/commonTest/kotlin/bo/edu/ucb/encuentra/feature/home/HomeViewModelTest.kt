package bo.edu.ucb.encuentra.feature.home

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.Campus
import bo.edu.ucb.encuentra.core.domain.model.UserModel
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.home.domain.model.HomeActivity
import bo.edu.ucb.encuentra.feature.home.domain.repository.HomeActivityRepository
import bo.edu.ucb.encuentra.feature.home.domain.usecase.ObserveHomeActivityUseCase
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeEffect
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeEvent
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val mateo = SeedData.users.first { it.id == "u-mateo" }

    /** Simula Room: cada valor nuevo es como un cambio en la base. */
    private val activity = MutableStateFlow(HomeActivity(activeReports = 2, newMatches = 1))
    private val requestedUserIds = mutableListOf<String>()

    private val repository = object : HomeActivityRepository {
        override fun observe(userId: String) = activity.also { requestedUserIds += userId }
    }

    private fun sessionWith(user: UserModel?) = object : SessionRepository {
        override fun getCurrentUser() = user
        override suspend fun setCurrentUser(user: UserModel) = Unit
        override suspend fun clearSession() = Unit
        override suspend fun restore() = Unit
    }

    private fun viewModelFor(user: UserModel?): HomeViewModel {
        val session = sessionWith(user)
        return HomeViewModel(GetCurrentUserUseCase(session), ObserveHomeActivityUseCase(repository, session))
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun headerUsesFirstNameFacultyAndCampusOfTheUser() {
        val state = viewModelFor(mateo).state.value
        assertEquals("¡Hola, Mateo!", state.greeting)
        assertEquals("Ingeniería • Sede La Paz", state.subtitle)

        val cochabamba = mateo.copy(campus = Campus.COCHABAMBA, faculty = null)
        assertEquals("Sede Cochabamba", viewModelFor(cochabamba).state.value.subtitle)
    }

    @Test
    fun activityComesFromTheCurrentUserAndUpdatesWhenRoomChanges() = runTest(dispatcher) {
        val viewModel = viewModelFor(mateo)
        advanceUntilIdle()
        assertEquals(listOf("u-mateo"), requestedUserIds)
        assertEquals("2 reportes activos", viewModel.state.value.activeReportsText)
        assertEquals("1 coincidencia nueva", viewModel.state.value.newMatchesText)
        assertTrue(viewModel.state.value.hasNewMatches)
        assertFalse(viewModel.state.value.loading)

        activity.value = HomeActivity(activeReports = 1, newMatches = 0)
        advanceUntilIdle()
        assertEquals("1 reporte activo", viewModel.state.value.activeReportsText)
        assertEquals("0 coincidencias nuevas", viewModel.state.value.newMatchesText)
        assertFalse(viewModel.state.value.hasNewMatches)
    }

    @Test
    fun withoutSessionActivityIsEmpty() = runTest(dispatcher) {
        val viewModel = viewModelFor(null)
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.activeReports)
        assertTrue(requestedUserIds.isEmpty())
    }

    @Test
    fun eventsNavigateToTheRightScreens() = runTest(dispatcher) {
        val viewModel = viewModelFor(mateo)
        val expected = mapOf(
            HomeEvent.OnReportObject to HomeEffect.NavigateToReportWizard,
            HomeEvent.OnActiveReportsClick to HomeEffect.NavigateToMyReports,
            HomeEvent.OnNewMatchesClick to HomeEffect.NavigateToMatches,
            HomeEvent.OnMyReportsClick to HomeEffect.NavigateToMyReports,
            HomeEvent.OnFoundItemsClick to HomeEffect.NavigateToFoundCatalog,
            HomeEvent.OnHowItWorksClick to HomeEffect.NavigateToHowItWorks
        )
        expected.forEach { (event, effect) ->
            val received = async { viewModel.effects.first() }
            advanceUntilIdle()
            viewModel.handleEvent(event)
            advanceUntilIdle()
            assertEquals(effect, received.await(), "Evento $event")
        }
    }
}
