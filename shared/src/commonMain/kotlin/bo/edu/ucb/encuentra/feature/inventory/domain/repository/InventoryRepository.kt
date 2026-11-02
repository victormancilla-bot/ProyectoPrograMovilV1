package bo.edu.ucb.encuentra.feature.inventory.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.inventory.domain.model.CustodyEntry
import kotlinx.coroutines.flow.Flow

/** Inventario de objetos encontrados (H07), en vivo desde Room. */
interface InventoryRepository {
    fun observeAll(): Flow<List<FoundItem>>

    fun observeItem(id: ItemId): Flow<FoundItem?>

    /** Historial de custodia con el nombre de quién movió el objeto, del más reciente al más antiguo. */
    fun observeHistory(id: ItemId): Flow<List<CustodyEntry>>

    suspend fun userName(userId: String): String?
}
