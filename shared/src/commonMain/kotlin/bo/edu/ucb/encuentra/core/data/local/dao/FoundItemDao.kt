package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FoundItemDao {
    @Query("SELECT * FROM found_items ORDER BY found_at DESC")
    fun observeAll(): Flow<List<FoundItemEntity>>

    @Query("SELECT * FROM found_items WHERE status = :status ORDER BY found_at DESC")
    fun observeByStatus(status: ItemStatus): Flow<List<FoundItemEntity>>

    @Query("SELECT * FROM found_items WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<FoundItemEntity?>

    @Query("SELECT * FROM found_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoundItemEntity?

    /** Para el matching (M01): objetos con los que comparar un reporte nuevo. */
    @Query("SELECT * FROM found_items WHERE status = :status")
    suspend fun getByStatus(status: ItemStatus): List<FoundItemEntity>

    /** Para el dashboard (D01) y la retención (D04): objetos en custodia que vencen antes de [until]. */
    @Query("SELECT * FROM found_items WHERE status = 'DISPONIBLE' AND retention_until <= :until ORDER BY retention_until")
    suspend fun getAvailableExpiringBefore(until: Long): List<FoundItemEntity>

    @Query("SELECT COUNT(*) FROM found_items WHERE status = :status")
    fun observeCountByStatus(status: ItemStatus): Flow<Int>

    @Upsert
    suspend fun upsert(item: FoundItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<FoundItemEntity>)

    @Query("UPDATE found_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ItemStatus)

    /** Lugar actual; el historial queda en custody_events (H05). */
    @Query("UPDATE found_items SET custody_place = :place, custody_detail = :detail WHERE id = :id")
    suspend fun updateCustody(id: String, place: CustodyPlace, detail: String)
}
