package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LostReportDao {
    @Query("SELECT * FROM lost_reports WHERE owner_id = :ownerId ORDER BY created_at DESC")
    fun observeByOwner(ownerId: String): Flow<List<LostReportEntity>>

    @Query("SELECT * FROM lost_reports WHERE owner_id = :ownerId AND status = :status ORDER BY created_at DESC")
    fun observeByOwnerAndStatus(ownerId: String, status: ReportStatus): Flow<List<LostReportEntity>>

    @Query("SELECT * FROM lost_reports WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<LostReportEntity?>

    @Query("SELECT * FROM lost_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LostReportEntity?

    @Query("SELECT * FROM lost_reports WHERE status = :status")
    suspend fun getByStatus(status: ReportStatus): List<LostReportEntity>

    @Upsert
    suspend fun upsert(report: LostReportEntity)

    @Upsert
    suspend fun upsertAll(reports: List<LostReportEntity>)

    @Query("UPDATE lost_reports SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ReportStatus)
}
