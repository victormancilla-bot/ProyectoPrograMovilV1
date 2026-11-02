package bo.edu.ucb.encuentra.feature.reportwizard

import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.Subcategory
import bo.edu.ucb.encuentra.feature.foundform.BoliviaTime
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ReportFieldError
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEffect
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardEvent
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportWizardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ReportWizardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ReportWizardViewModel(ValidateReportStepUseCase(FixedClock, BoliviaTime))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun send(vararg events: ReportWizardEvent) = events.forEach(viewModel::handleEvent)
    private val state get() = viewModel.state.value

    @Test
    fun nextIsDisabledUntilCategoryAndSubcategoryAreChosen() {
        assertFalse(state.canContinue)
        send(ReportWizardEvent.OnNext)
        assertEquals(1, state.step)

        send(ReportWizardEvent.OnCategorySelected(Category.ELECTRONICA))
        assertEquals(setOf(ReportFieldError.SUBCATEGORY_REQUIRED), state.errors)

        send(ReportWizardEvent.OnSubcategorySelected(Subcategory.LAPTOP), ReportWizardEvent.OnNext)
        assertEquals(2, state.step)
        assertEquals("Atributos", state.stepName)
    }

    @Test
    fun changingCategoryClearsASubcategoryThatNoLongerFits() {
        send(
            ReportWizardEvent.OnCategorySelected(Category.ELECTRONICA),
            ReportWizardEvent.OnSubcategorySelected(Subcategory.LAPTOP),
            ReportWizardEvent.OnCategorySelected(Category.LLAVES)
        )
        assertNull(state.draft.subcategory)
        assertFalse(state.canContinue)
    }

    @Test
    fun attributesStepNeedsTitleAndColor() {
        send(
            ReportWizardEvent.OnCategorySelected(Category.ELECTRONICA),
            ReportWizardEvent.OnSubcategorySelected(Subcategory.LAPTOP),
            ReportWizardEvent.OnNext
        )
        assertFalse(state.canContinue)

        send(ReportWizardEvent.OnTitleChanged("Laptop Dell XPS"))
        assertEquals(setOf(ReportFieldError.COLOR_REQUIRED), state.errors)

        send(ReportWizardEvent.OnColorSelected("Plateado"), ReportWizardEvent.OnBrandChanged("Dell"))
        assertTrue(state.canContinue)
        send(ReportWizardEvent.OnNext)
        assertEquals(3, state.step)
    }

    @Test
    fun goingBackKeepsTheDraft() {
        send(
            ReportWizardEvent.OnCategorySelected(Category.LLAVES),
            ReportWizardEvent.OnSubcategorySelected(Subcategory.LLAVERO),
            ReportWizardEvent.OnNext,
            ReportWizardEvent.OnTitleChanged("Llavero UCB"),
            ReportWizardEvent.OnBack
        )
        assertEquals(1, state.step)
        assertEquals(Subcategory.LLAVERO, state.draft.subcategory)
        assertEquals("Llavero UCB", state.draft.title)
    }

    @Test
    fun backOnTheFirstStepCancelsTheWizard() = runTest(dispatcher) {
        val effect = async { viewModel.effects.first() }
        advanceUntilIdle()
        send(ReportWizardEvent.OnBack)
        advanceUntilIdle()
        assertEquals(ReportWizardEffect.Cancel, effect.await())
    }
}
