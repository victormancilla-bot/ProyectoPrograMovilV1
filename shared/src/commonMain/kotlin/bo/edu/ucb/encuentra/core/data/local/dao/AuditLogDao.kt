package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import bo.edu.ucb.encuentra.core.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

/** Auditoría append-only (S05): a propósito NO hay update ni delete. */
@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY at DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE target_id = :targetId ORDER BY at DESC")
    fun observeByTarget(targetId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE actor_id = :actorId ORDER BY at DESC")
    fun observeByActor(actorId: String): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(log: AuditLogEntity)

    @Insert
    suspend fun insertAll(logs: List<AuditLogEntity>)
}
