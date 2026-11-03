package bo.edu.ucb.encuentra.feature.audit.domain.usecase

import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.audit.domain.model.AuditEntry
import bo.edu.ucb.encuentra.feature.audit.domain.repository.AuditTrailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** S05 · Visor de auditoría: solo el staff (y admin) la consulta; para el resto queda vacía. */
class ObserveAuditTrailUseCase(
    private val repository: AuditTrailRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<AuditEntry>> =
        if (getCurrentUser()?.role?.isStaff == true) {
            repository.observeEntries().map { entries -> entries.sortedWith(compareByDescending<AuditEntry> { it.at }.thenByDescending { it.id }) }
        } else {
            flowOf(emptyList())
        }
}
