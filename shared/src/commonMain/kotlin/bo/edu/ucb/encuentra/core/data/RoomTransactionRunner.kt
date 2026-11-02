package bo.edu.ucb.encuentra.core.data

import androidx.room3.withWriteTransaction
import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner

/** Los DAOs llamados dentro de [run] comparten la transacción de escritura de Room. */
class RoomTransactionRunner(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withWriteTransaction { block() }
}
