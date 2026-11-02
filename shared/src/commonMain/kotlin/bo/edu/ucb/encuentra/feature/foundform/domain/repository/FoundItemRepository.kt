package bo.edu.ucb.encuentra.feature.foundform.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.FoundItem

interface FoundItemRepository {
    /** Guarda el objeto junto con su primer evento de custodia y el registro de auditoría, todo o nada. */
    suspend fun register(item: FoundItem)
}
