package bo.edu.ucb.encuentra.feature.myreports.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.myreports.domain.model.CloseReason
import bo.edu.ucb.encuentra.feature.myreports.domain.model.ReportDetail
import bo.edu.ucb.encuentra.feature.myreports.domain.model.isActive
import bo.edu.ucb.encuentra.feature.myreports.domain.repository.MyReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Mis reportes (L05): en curso primero y, dentro de cada grupo, el más reciente primero. */
class ObserveMyReportsUseCase(
    private val repository: MyReportsRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    operator fun invoke(): Flow<List<LostReport>> {
        val user = getCurrentUser() ?: return flowOf(emptyList())
        return repository.observeByOwner(user.id).map { reports ->
            reports.sortedWith(
                compareByDescending<LostReport> { it.status.isActive }.thenByDescending { it.createdAt }
            )
        }
    }
}

/** Detalle de un reporte con sus coincidencias (null si no existe). */
class ObserveReportDetailUseCase(private val repository: MyReportsRepository) {
    operator fun invoke(reportId: ItemId): Flow<ReportDetail?> = combine(
        repository.observeReport(reportId),
        repository.observeMatches(reportId)
    ) { report, matches ->
        report?.let { ReportDetail(it, matches.sortedByDescending { match -> match.score }) }
    }
}

/**
 * Cerrar o cancelar un reporte (L05): solo su dueño y solo si está BUSCANDO (en verificación o con match
 * confirmado lo cierra el flujo de reclamo y entrega). Al dejar de estar BUSCANDO el matching ya no lo toca.
 */
class CloseReportUseCase(
    private val repository: MyReportsRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val audit: AuditRecorder
) {
    suspend operator fun invoke(reportId: ItemId, reason: CloseReason): Result<Unit> = runCatching {
        val user = getCurrentUser() ?: throw IllegalStateException(NO_SESSION)
        val report = repository.getReport(reportId) ?: throw NoSuchElementException(NOT_FOUND)
        if (report.ownerId != user.id) throw IllegalStateException(NOT_OWNER)
        if (report.status != ReportStatus.BUSCANDO) {
            throw IllegalStateException(NOT_CLOSABLE)
        }
        repository.updateStatus(reportId, reason.status)
        audit.record(
            action = AuditAction.REPORT_CLOSED,
            actorId = user.id,
            targetId = reportId.value,
            detail = "${report.title} · ${reason.label}"
        )
    }

    companion object {
        const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
        const val NOT_FOUND = "El reporte no existe"
        const val NOT_OWNER = "Solo puedes cerrar tus propios reportes"
        const val NOT_CLOSABLE = "Este reporte ya está en verificación o cerrado"
    }
}
