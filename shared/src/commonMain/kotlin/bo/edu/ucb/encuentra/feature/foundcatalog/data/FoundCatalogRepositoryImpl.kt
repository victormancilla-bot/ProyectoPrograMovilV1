package bo.edu.ucb.encuentra.feature.foundcatalog.data

import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.repository.FoundCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoundCatalogRepositoryImpl(private val dao: FoundItemDao) : FoundCatalogRepository {
    override fun observeAvailable(): Flow<List<CatalogItem>> =
        dao.observeByStatus(ItemStatus.DISPONIBLE).map { list -> list.map { it.toModel().toCatalogItem() } }
}

/** El recorte se hace aquí: lo que no está en CatalogItem nunca llega a la pantalla. */
internal fun FoundItem.toCatalogItem() = CatalogItem(
    id = id.value,
    category = category,
    subcategory = subcategory,
    zone = foundZone,
    foundAt = foundAt,
    photoUrl = photoUrls.firstOrNull()
)
