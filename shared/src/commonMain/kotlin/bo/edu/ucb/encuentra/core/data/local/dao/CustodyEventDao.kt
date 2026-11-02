package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import bo.edu.ucb.encuentra.core.data.local.entity.CustodyEventEntity
import kotlinx.coroutines.flow.Flow

/** Historial de custodia: solo se agrega (es un historial, no se edita). */
@Dao
interface CustodyEventDao {
    @Query("SELECT * FROM custody_events WHERE found_item_id = :foundItemId ORDER BY at DESC")
    fun observeByFoundItem(foundItemId: String): Flow<List<CustodyEventEntity>>

    @Insert
    suspend fun insert(event: CustodyEventEntity)

    @Insert
    suspend fun insertAll(events: List<CustodyEventEntity>)
}
