package bo.edu.ucb.encuentra.feature.inventory.domain.repository

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import kotlinx.coroutines.flow.Flow

/** Datos de la política de retención (D04). */
interface RetentionRepository {
    /** Objetos DISPONIBLES (los únicos que pueden vencer), para la pantalla. */
    fun observeAvailable(): Flow<List<FoundItem>>

    /** Lo mismo, una sola vez, para el chequeo en segundo plano. */
    suspend fun availableItems(): List<FoundItem>

    suspend fun findItem(id: String): FoundItem?

    /** Ids de STAFF y ADMIN: a quiénes se avisa (canal Retención de N01). */
    suspend fun staffIds(): List<String>
}
