package bo.edu.ucb.encuentra.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import bo.edu.ucb.encuentra.core.data.local.entity.MatchEntity
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    /** Coincidencias de un reporte, mejor score primero, sin las descartadas ("No es mío", M02). */
    @Query(
        "SELECT * FROM matches WHERE lost_report_id = :lostReportId AND status != 'DESCARTADO' ORDER BY score DESC"
    )
    fun observeActiveByLostReport(lostReportId: String): Flow<List<MatchEntity>>

    /** Coincidencias no descartadas de todos los reportes de un usuario (Posibles coincidencias, M04). */
    @Query(
        "SELECT m.* FROM matches m JOIN lost_reports r ON m.lost_report_id = r.id " +
            "WHERE r.owner_id = :ownerId AND m.status != 'DESCARTADO' ORDER BY m.score DESC"
    )
    fun observeActiveByOwner(ownerId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE found_item_id = :foundItemId ORDER BY score DESC")
    suspend fun getByFoundItem(foundItemId: String): List<MatchEntity>

    @Upsert
    suspend fun upsert(match: MatchEntity)

    @Upsert
    suspend fun upsertAll(matches: List<MatchEntity>)

    @Query("UPDATE matches SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: MatchStatus)
}
