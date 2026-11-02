package bo.edu.ucb.encuentra.feature.inventory.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.retentionState
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.model.InventoryItemDetail
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Todos los objetos encontrados con su estado de retención (H07). Se actualiza solo cuando cambia Room. */
class ObserveInventoryUseCase(
    private val repository: InventoryRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<List<InventoryEntry>> = repository.observeAll().map { items ->
        val now = clock.now()
        items.map { InventoryEntry(it, it.retentionState(now)) }
    }
}

/**
 * Detalle de un objeto (H07): datos completos (incluida la custodia, que el estudiante no ve), quién lo registró
 * e historial de custodia. Editar la custodia es `MoveCustodyUseCase` (H05), que también la audita.
 */
class ObserveInventoryItemUseCase(
    private val repository: InventoryRepository,
    private val clock: Clock
) {
    operator fun invoke(id: ItemId): Flow<InventoryItemDetail?> =
        combine(repository.observeItem(id), repository.observeHistory(id)) { item, history ->
            item?.let {
                InventoryItemDetail(
                    entry = InventoryEntry(it, it.retentionState(clock.now())),
                    registeredByName = repository.userName(it.registeredBy),
                    history = history
                )
            }
        }
}
