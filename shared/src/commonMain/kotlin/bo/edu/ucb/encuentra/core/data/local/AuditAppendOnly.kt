package bo.edu.ucb.encuentra.core.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * S05 · La auditoría es append-only también a nivel de base de datos: aunque alguien agregue un @Update/@Delete
 * al DAO o escriba SQL a mano, SQLite rechaza el cambio. Se crean al abrir la base (IF NOT EXISTS), así no hace
 * falta una migración de esquema y aplica también a las bases que ya existían.
 */
object AuditAppendOnly : RoomDatabase.Callback() {

    const val MESSAGE = "audit_logs es append-only: no se puede modificar ni borrar"

    val TRIGGERS = listOf(
        "CREATE TRIGGER IF NOT EXISTS audit_logs_no_update BEFORE UPDATE ON audit_logs " +
            "BEGIN SELECT RAISE(ABORT, '$MESSAGE'); END",
        "CREATE TRIGGER IF NOT EXISTS audit_logs_no_delete BEFORE DELETE ON audit_logs " +
            "BEGIN SELECT RAISE(ABORT, '$MESSAGE'); END"
    )

    override suspend fun onOpen(connection: SQLiteConnection) {
        TRIGGERS.forEach(connection::execSQL)
    }
}
