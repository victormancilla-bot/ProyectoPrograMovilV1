package bo.edu.ucb.encuentra.core.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlinx.coroutines.flow.Flow

/** Ciclo de vida de un objeto encontrado (H05): estado e historial de custodia. */
interface FoundItemLifecycleRepository {
    suspend fun getById(id: ItemId): FoundItem?

    suspend fun updateStatus(id: ItemId, status: ItemStatus)

    /** Agrega [event] al historial y deja el objeto en ese lugar. */
    suspend fun moveCustody(event: CustodyEvent)

    /** Dónde estuvo guardado, del más reciente al más antiguo. */
    fun observeCustodyHistory(id: ItemId): Flow<List<CustodyEvent>>
}
