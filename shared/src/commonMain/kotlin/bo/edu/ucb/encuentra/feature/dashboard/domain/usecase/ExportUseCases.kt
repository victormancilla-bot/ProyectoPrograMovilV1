package bo.edu.ucb.encuentra.feature.dashboard.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportBuilder
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportData
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportFile
import bo.edu.ucb.encuentra.feature.dashboard.domain.model.ExportRequest
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/** D03 · Datos exportables: solo staff (el inventario completo incluye custodia y descripciones privadas). */
class ObserveExportDataUseCase(
    private val repository: ExportRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<ExportData> =
        if (getCurrentUser()?.role?.isStaff == true) repository.observeData()
        else flowOf(ExportData(emptyList(), emptyMap(), emptyList()))
}

/** Arma el CSV y deja constancia: sacar datos de la app también queda en la auditoría. */
class BuildExportUseCase(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val audit: AuditRecorder,
    private val clock: Clock,
    private val timeZone: TimeZone
) {
    suspend operator fun invoke(data: ExportData, request: ExportRequest): Result<ExportFile> = runCatching {
        val user = getCurrentUser()?.takeIf { it.role.isStaff } ?: error(NOT_STAFF)
        val file = ExportBuilder.build(data, request, clock.now(), timeZone)
        audit.record(EXPORT_ACTION, user.id, request.kind.fileName, "${file.name} · ${file.rows} filas")
        file
    }

    companion object {
        const val NOT_STAFF = "Solo el staff puede exportar datos."
        val EXPORT_ACTION = AuditAction.DATA_EXPORTED
    }
}
