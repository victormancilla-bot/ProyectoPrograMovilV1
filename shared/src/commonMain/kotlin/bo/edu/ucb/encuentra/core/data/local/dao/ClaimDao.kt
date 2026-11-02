package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.ClaimEntity
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims WHERE status = :status ORDER BY created_at DESC")
    fun observeByStatus(status: ClaimStatus): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE claimant_id = :claimantId ORDER BY created_at DESC")
    fun observeByClaimant(claimantId: String): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ClaimEntity?>

    @Query("SELECT * FROM claims WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ClaimEntity?

    /** Todos los reclamos de un objeto (V02: varias personas pueden reclamarlo; el staff decide). */
    @Query("SELECT * FROM claims WHERE found_item_id = :foundItemId ORDER BY created_at")
    suspend fun getByFoundItem(foundItemId: String): List<ClaimEntity>

    @Query("SELECT * FROM claims WHERE found_item_id = :foundItemId")
    fun observeByFoundItem(foundItemId: String): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE claimant_id = :claimantId")
    suspend fun getByClaimant(claimantId: String): List<ClaimEntity>

    @Query("SELECT COALESCE(MAX(number), 1000) + 1 FROM claims")
    suspend fun nextNumber(): Int

    /** Falla (índice único) si la misma persona ya reclamó ese objeto: V02 bloquea el doble reclamo. */
    @Insert
    suspend fun insert(claim: ClaimEntity)

    @Upsert
    suspend fun upsertAll(claims: List<ClaimEntity>)

    @Query("UPDATE claims SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ClaimStatus)
}
