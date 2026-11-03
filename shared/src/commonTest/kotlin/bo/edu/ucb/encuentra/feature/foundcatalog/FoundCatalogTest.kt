package bo.edu.ucb.encuentra.feature.foundcatalog

import bo.edu.ucb.encuentra.core.FixedAppConfig
import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.AppConfig
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.domain.model.Category
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.data.toCatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.repository.FoundCatalogRepository
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.GetHowItWorksUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.ObserveFoundCatalogUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogEffect
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogEvent
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogViewModel
import bo.edu.ucb.encuentra.feature.matching.foundItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

/** Los DISPONIBLES del seed, como los entrega el repositorio de Room. */
private class SeedCatalog : FoundCatalogRepository {
    override fun observeAvailable() =
        flowOf(SeedData.foundItems.filter { it.status == ItemStatus.DISPONIBLE }.map { it.toCatalogItem() })
}

@OptIn(ExperimentalCoroutinesApi::class)
class FoundCatalogTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun catalogItemKeepsOnlyCategoryZoneDateAndPhoto() {
        val item = foundItem(id = "fi-x", title = "Calculadora de Mateo Quispe", description = "Con su nombre en la tapa")
            .copy(photoUrls = listOf("file://a.jpg", "file://b.jpg"))

        val public = item.toCatalogItem()

        assertEquals(CatalogItem("fi-x", item.category, item.subcategory, item.foundZone, item.foundAt, "file://a.jpg"), public)
        // Nada del título, la descripción, el color, la marca, la custodia ni las preguntas.
        val shown = public.toString()
        listOf("Mateo", "tapa", "Negro", "Casio", "OFICINA", "¿Qué nombre").forEach { assertFalse(it in shown, it) }
    }

    @Test
    fun onlyAvailableItemsNewestFirst() = runTest(dispatcher) {
        val items = ObserveFoundCatalogUseCase(SeedCatalog())().first()
        assertEquals(
            listOf("fi-cuaderno-tapa-dura", "fi-termo", "fi-llavero", "fi-chamarra", "fi-paraguas"),
            items.map { it.id }
        )
    }

    @Test
    fun filtersByCategoryAndZoneAndOnlyOffersOptionsWithItems() = runTest(dispatcher) {
        val vm = FoundCatalogViewModel(ObserveFoundCatalogUseCase(SeedCatalog()))
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(5, state.visible.size)
        assertEquals(listOf(Category.ROPA_ACCESORIOS, Category.LIBROS_CUADERNOS, Category.LLAVES, Category.OTROS), state.categories)
        assertFalse(CampusZone.BLOQUE_A in state.zones)

        vm.handleEvent(FoundCatalogEvent.OnCategorySelected(Category.ROPA_ACCESORIOS))
        assertEquals(setOf("fi-termo", "fi-chamarra"), vm.state.value.visible.map { it.id }.toSet())

        vm.handleEvent(FoundCatalogEvent.OnZoneSelected(CampusZone.CAFETERIA))
        assertEquals(listOf("fi-termo"), vm.state.value.visible.map { it.id })
        assertTrue(vm.state.value.hasFilters)

        vm.handleEvent(FoundCatalogEvent.OnClearFilters)
        assertEquals(5, vm.state.value.visible.size)
    }

    @Test
    fun itIsYoursLeadsToTheReportWizard() = runTest(dispatcher) {
        val vm = FoundCatalogViewModel(ObserveFoundCatalogUseCase(SeedCatalog()))
        val effect = async { vm.effects.first() }
        advanceUntilIdle()
        vm.handleEvent(FoundCatalogEvent.OnReport)
        advanceUntilIdle()
        assertEquals(FoundCatalogEffect.NavigateToReport, effect.await())
    }

    @Test
    fun howItWorksHasFourStepsWithTheRetentionDaysFromRemoteConfig() {
        val steps = GetHowItWorksUseCase(GetAppConfigUseCase(FixedAppConfig(AppConfig(retentionDays = 60)))).invoke()
        assertEquals(4, steps.size)
        assertEquals("Recógelo con tu carnet", steps.last().title)
        assertTrue("60 días" in steps.last().body)
    }
}
