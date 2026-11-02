package bo.edu.ucb.encuentra.feature.foundform

import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.Role
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.FoundFieldError
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.RegisterFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormEffect
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormEvent
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FoundFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeFoundItemRepository()
    private lateinit var viewModel: FoundFormViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val validate = ValidateFoundItemUseCase(FixedClock, BoliviaTime)
        val register = RegisterFoundItemUseCase(
            validate, repository, GetCurrentUserUseCase(FakeSession(demoUser(Role.STAFF))), FixedClock
        )
        viewModel = FoundFormViewModel(validate, register, FixedClock, BoliviaTime)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun send(vararg events: FoundFormEvent) = events.forEach(viewModel::handleEvent)
    private val state get() = viewModel.state.value

    private fun fillValidForm() = send(
        FoundFormEvent.OnCategorySelected(Category.ELECTRONICA),
        FoundFormEvent.OnSubcategorySelected(Subcategory.CELULAR),
        FoundFormEvent.OnColorSelected("Negro"),
        FoundFormEvent.OnTitleChanged("Smartphone Negro"),
        FoundFormEvent.OnFoundZoneSelected(CampusZone.BIBLIOTECA_CENTRAL),
        FoundFormEvent.OnCustodySelected(CustodyPlace.OFICINA),
        FoundFormEvent.OnQuestionChanged(0, "¿Qué fondo de pantalla tiene?")
    )

    @Test
    fun dateAndTimeStartAtNow() {
        assertEquals(LocalDate(2026, 11, 2), state.draft.foundDate)
        assertEquals(LocalTime(12, 0), state.draft.foundTime)
    }

    @Test
    fun errorsAreShownOnlyAfterTryingToRegister() = runTest(dispatcher) {
        assertNull(state.errorFor(FoundFieldError.CUSTODY_REQUIRED))
        send(FoundFormEvent.OnSubmit)
        advanceUntilIdle()
        assertTrue(state.showErrors)
        assertEquals(FoundFieldError.CUSTODY_REQUIRED.message, state.errorFor(FoundFieldError.CUSTODY_REQUIRED))
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun controlQuestionsGoFromOneToThree() {
        send(FoundFormEvent.OnRemoveQuestion(0))
        assertEquals(1, state.draft.controlQuestions.size)
        send(FoundFormEvent.OnAddQuestion, FoundFormEvent.OnAddQuestion, FoundFormEvent.OnAddQuestion)
        assertEquals(3, state.draft.controlQuestions.size)
        assertFalse(state.canAddQuestion)
        send(FoundFormEvent.OnQuestionChanged(2, "¿Qué tiene en la funda?"), FoundFormEvent.OnRemoveQuestion(1))
        assertEquals(listOf("", "¿Qué tiene en la funda?"), state.draft.controlQuestions)
    }

    @Test
    fun validFormIsSavedAsAvailableAndEmitsRegistered() = runTest(dispatcher) {
        // "Ahora" es 12:00: se registra un hallazgo de la mañana.
        fillValidForm()
        send(FoundFormEvent.OnFoundTimeSelected(LocalTime(9, 30)))
        assertTrue(state.errors.isEmpty())

        val effect = async { viewModel.effects.first() }
        advanceUntilIdle()
        send(FoundFormEvent.OnSubmit)
        advanceUntilIdle()

        assertEquals(FoundFormEffect.Registered("Smartphone Negro"), effect.await())
        assertEquals(ItemStatus.DISPONIBLE, repository.saved.single().status)
        assertFalse(state.saving)
    }
}
