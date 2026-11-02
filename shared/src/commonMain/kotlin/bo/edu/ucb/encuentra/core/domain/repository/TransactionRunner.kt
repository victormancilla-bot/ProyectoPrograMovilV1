package bo.edu.ucb.encuentra.core.domain.repository

/** Ejecuta varios cambios como uno solo (todo o nada) sin que el dominio conozca Room. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
