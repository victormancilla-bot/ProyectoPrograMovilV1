package bo.edu.ucb.encuentra.feature.dashboard.domain.usecase

import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ItemOutcome
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** D02 · Datos de las métricas: solo para el staff (el estudiante no ve estadísticas de la oficina). */
class ObserveMetricsUseCase(
    private val repository: MetricsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<ItemOutcome>> =
        if (getCurrentUser()?.role?.isStaff == true) repository.observeOutcomes() else flowOf(emptyList())
}
