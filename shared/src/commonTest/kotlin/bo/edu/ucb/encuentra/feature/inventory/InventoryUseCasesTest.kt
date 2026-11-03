package bo.edu.ucb.encuentra.feature.inventory

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.RetentionState
import bo.edu.ucb.encuentra.core.domain.model.code
import bo.edu.ucb.encuentra.core.domain.model.retentionState
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.FixedClock
import bo.edu.ucb.encuentra.feature.inventory.domain.model.CustodyEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryFilter
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventorySearch
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.InventoryRepository
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryItemUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/** Inventario del seed 2026 en memoria (con el historial de custodia del seed). */
internal class SeedInventoryRepository : InventoryRepository {
    val items = MutableStateFlow(SeedData.foundItems)
    val history = MutableStateFlow(SeedData.custodyEvents)
    private val names = SeedData.users.associate { it.id to it.fullName }

    override fun observeAll(): Flow<List<FoundItem>> = items
    override fun observeItem(id: ItemId) = items.map { all -> all.firstOrNull { it.id == id } }
    override fun observeHistory(id: ItemId) = history.map { events ->
        events.filter { it.foundItemId == id }.sortedByDescending { it.at }.map { CustodyEntry(it, names[it.changedBy]) }
    }
    override suspend fun userName(userId: String) = names[userId]

    fun set(id: String, change: (FoundItem) -> FoundItem) {
        items.value = items.value.map { if (it.id.value == id) change(it) else it }
    }
}

class InventoryUseCasesTest {

    private val repository = SeedInventoryRepository()
    private val now = FixedClock.now()

    private suspend fun entries() = ObserveInventoryUseCase(repository, FixedClock)().first()

    @Test
    fun retentionStateIsCalculatedOnlyForAvailableItems() {
        val llavero = SeedData.foundItems.first { it.id.value == "fi-llavero" }
        // El llavero (seed) vence el 6 nov: a 4 días del "ahora" fijo (2 nov).
        assertEquals(RetentionState.POR_VENCER, llavero.retentionState(now))
        assertEquals(RetentionState.VENCIDO, llavero.retentionState(now + 5.days))
        assertEquals(RetentionState.VIGENTE, llavero.retentionState(now - 30.days))
        // Uno reclamado espera su entrega: no "vence".
        assertEquals(RetentionState.VIGENTE, llavero.copy(status = ItemStatus.RECLAMADO).retentionState(now + 5.days))
    }

    @Test
    fun chipsFilterByStatusAndRetention() = runTest {
        repository.set("fi-termo") { it.copy(retentionUntil = now - 1.days) }
        val all = entries()

        assertEquals(8, InventorySearch.apply(all, InventoryFilter.TODOS, "").size)
        assertEquals(setOf("fi-calculadora", "fi-billetera", "fi-apuntes-fisica"), ids(InventorySearch.apply(all, InventoryFilter.RECLAMADO, "")))
        // El termo + los 2 vencidos del seed (D04).
        assertEquals(setOf("fi-termo", "fi-paraguas", "fi-chamarra"), ids(InventorySearch.apply(all, InventoryFilter.VENCIDO, "")))
        // Disponible = esperando a su dueño y sin vencer (el termo vencido no entra).
        assertEquals(setOf("fi-llavero", "fi-cuaderno-tapa-dura"), ids(InventorySearch.apply(all, InventoryFilter.DISPONIBLE, "")))
    }

    @Test
    fun whatNeedsAttentionComesFirst() = runTest {
        repository.set("fi-termo") { it.copy(retentionUntil = now - 1.days) }

        val ordered = InventorySearch.apply(entries(), InventoryFilter.TODOS, "")

        // Vencido, luego por vencer, luego el resto del más reciente al más antiguo.
        assertEquals(
            listOf("fi-termo", "fi-chamarra", "fi-paraguas", "fi-llavero", "fi-cuaderno-tapa-dura"),
            ordered.take(5).map { it.item.id.value }
        )
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndMatchesCodeBrandAndPlace() = runTest {
        val all = entries()

        assertEquals(setOf("fi-calculadora"), ids(InventorySearch.apply(all, InventoryFilter.TODOS, "CASIO")))
        assertEquals(setOf("fi-calculadora"), ids(InventorySearch.apply(all, InventoryFilter.TODOS, "científica")))
        assertEquals(setOf("fi-calculadora"), ids(InventorySearch.apply(all, InventoryFilter.TODOS, "#calculad")))
        assertEquals(setOf("fi-termo", "fi-cuaderno-tapa-dura"), ids(InventorySearch.apply(all, InventoryFilter.TODOS, "cafeteria")))
        // Varias palabras: todas deben estar.
        assertEquals(setOf("fi-cuaderno-tapa-dura"), ids(InventorySearch.apply(all, InventoryFilter.TODOS, "cuaderno moleskine")))
        assertTrue(InventorySearch.apply(all, InventoryFilter.TODOS, "bicicleta").isEmpty())
        assertEquals("#CALCULAD", SeedData.foundItems.first { it.id.value == "fi-calculadora" }.code)
    }

    @Test
    fun detailHasCustodyHistoryAndWhoRegisteredIt() = runTest {
        val detail = ObserveInventoryItemUseCase(repository, FixedClock)(ItemId("fi-llavero")).first()!!

        assertEquals("Ana Staff", detail.registeredByName)
        assertEquals(RetentionState.POR_VENCER, detail.entry.retention)
        // Seed: registro en oficina y luego movido a locker (el más reciente primero).
        assertEquals(2, detail.history.size)
        assertTrue(detail.history.first().event.at >= detail.history.last().event.at)
        assertTrue(detail.canEditCustody)
    }

    @Test
    fun itemsOutOfCustodyCannotBeMoved() = runTest {
        repository.set("fi-llavero") { it.copy(status = ItemStatus.ENTREGADO) }
        val detail = ObserveInventoryItemUseCase(repository, FixedClock)(ItemId("fi-llavero")).first()!!
        assertFalse(detail.canEditCustody)
        assertNull(ObserveInventoryItemUseCase(repository, FixedClock)(ItemId("fi-no-existe")).first())
    }

    private fun ids(entries: List<InventoryEntry>) =
        entries.map { it.item.id.value }.toSet()
}
