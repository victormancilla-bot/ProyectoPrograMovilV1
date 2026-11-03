package bo.edu.ucb.encuentra.feature.foundcatalog.domain.repository

import bo.edu.ucb.encuentra.feature.foundcatalog.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow

interface FoundCatalogRepository {
    /** Objetos DISPONIBLES (en custodia y sin reclamar), ya recortados a la vista pública. */
    fun observeAvailable(): Flow<List<CatalogItem>>
}
