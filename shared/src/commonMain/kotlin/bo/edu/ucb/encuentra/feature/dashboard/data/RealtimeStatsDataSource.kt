package bo.edu.ucb.encuentra.feature.dashboard.data

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Claves del nodo `stats/` que se crean en la consola de Firebase (o que publica N03 desde el servidor). */
object StatsKeys {
    const val PATH = "stats"
    const val IN_CUSTODY = "en_custodia"
    const val PENDING_DELIVERY = "pendientes_entrega"
    const val IN_VERIFICATION = "en_verificacion"
    const val EXPIRING_SOON = "por_vencer"
}

/**
 * Contadores del dashboard desde Firebase Realtime Database, como `RealTimeDataBase` de la feature
 * realtimedatabase del docente: `reference(...).valueEvents` emite cada vez que cambia el nodo, sin refrescar.
 * Firebase se toca recién al recolectar el Flow: si no está configurado, el error llega por el Flow.
 */
class RealtimeStatsDataSource {

    fun observe(): Flow<DashboardStats?> = flow {
        emitAll(Firebase.database.reference(StatsKeys.PATH).valueEvents.map { it.toStats() })
    }

    /** null si el nodo no existe o le falta algún contador: así se usan los de Room en vez de mostrar ceros. */
    private fun DataSnapshot.toStats(): DashboardStats? {
        if (!exists) return null
        fun count(key: String): Int? = (child(key).value as? Number)?.toInt()?.coerceAtLeast(0)
        return DashboardStats(
            inCustody = count(StatsKeys.IN_CUSTODY) ?: return null,
            pendingDelivery = count(StatsKeys.PENDING_DELIVERY) ?: return null,
            inVerification = count(StatsKeys.IN_VERIFICATION) ?: return null,
            expiringSoon = count(StatsKeys.EXPIRING_SOON) ?: return null
        )
    }
}
