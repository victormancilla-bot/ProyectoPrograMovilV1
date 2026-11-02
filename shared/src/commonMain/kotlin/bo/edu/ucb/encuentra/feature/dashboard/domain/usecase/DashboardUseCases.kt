package bo.edu.ucb.encuentra.feature.dashboard.domain.usecase

import bo.edu.ucb.encuentra.feature.dashboard.domain.model.DashboardStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.LiveStats
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.PendingClaim
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.StatsSource
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Contadores en vivo del Dashboard (D01), sin refrescar:
 * - **Realtime Database** (`stats/`) cuando Firebase está configurado y el nodo existe.
 * - **Room** mientras tanto (primero se muestra lo local, así nunca se queda cargando sin internet) o si
 *   Firebase no está disponible: los COUNT de Room también se actualizan solos.
 * "Por vencer" depende de la hora: se recalcula cada [tick].
 */
class ObserveDashboardStatsUseCase(
    private val repository: DashboardRepository,
    /** "Ahora", re-emitido periódicamente. Los tests pasan un instante fijo para no esperar un reloj infinito. */
    private val ticks: Flow<Instant>
) {
    constructor(repository: DashboardRepository, clock: Clock, tick: Duration = 30.minutes) :
        this(repository, clockTicks(clock, tick))

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<LiveStats> =
        repository.observeRealtimeStats()
            .onStart { emit(null) }
            // Sin google-services.json (o sin permisos) se sigue con Room.
            .catch { error -> if (error is CancellationException) throw error else emit(null) }
            .distinctUntilChanged()
            .flatMapLatest { remote ->
                if (remote != null) flowOf(LiveStats(remote, StatsSource.REALTIME_DATABASE)) else localStats()
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun localStats(): Flow<LiveStats> =
        ticks.flatMapLatest { now ->
            repository.observeLocalStats(expiringBefore = now + DashboardStats.EXPIRING_WINDOW_DAYS.days)
        }.map { LiveStats(it, StatsSource.LOCAL) }
}

/** La hora actual cada [tick] (para que "por vencer" avance aunque no cambie la base). */
internal fun clockTicks(clock: Clock, tick: Duration): Flow<Instant> = flow {
    while (true) {
        emit(clock.now())
        delay(tick)
    }
}

/** "Últimos reclamos pendientes" del Dashboard: los 3 más recientes; "Ver todos" abre la lista completa. */
class ObserveLatestPendingClaimsUseCase(private val repository: DashboardRepository) {
    operator fun invoke(limit: Int = LIMIT): Flow<List<PendingClaim>> = repository.observeLatestPendingClaims(limit)

    companion object {
        const val LIMIT = 3
    }
}
