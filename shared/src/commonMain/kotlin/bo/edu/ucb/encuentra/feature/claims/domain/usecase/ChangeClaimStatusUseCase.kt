package bo.edu.ucb.encuentra.feature.claims.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemStateMachine
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemTransition
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.Match
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimStateMachine
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimTransition
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import kotlin.time.Clock

/**
 * Aplica una [ClaimTransition] validando con [ClaimStateMachine], con sus efectos, todo en una transacción:
 *
 * | Transición | Reclamo | Coincidencia | Reporte del dueño | Objeto (H05) |
 * |---|---|---|---|---|
 * | SUBMIT_ANSWERS (quien reclama) | guarda las respuestas | — | — | — |
 * | APPROVE (staff) | revisado por / cuándo | CONFIRMADO | Match confirmado | sigue RECLAMADO |
 * | REJECT (staff) | revisado por / cuándo | DESCARTADO | vuelve a Buscando si no tiene otro reclamo abierto | vuelve a DISPONIBLE si nadie más lo reclama |
 * | DELIVER (staff) | entregado por / cuándo | — | Recuperado | ENTREGADO; los otros reclamos abiertos se rechazan |
 *
 * Las features usan los use cases de cada transición (SubmitClaimAnswersUseCase, ApproveClaimUseCase...).
 */
class ChangeClaimStatusUseCase(
    private val claims: ClaimRepository,
    private val items: FoundItemLifecycleRepository,
    private val audit: AuditRecorder,
    private val transaction: TransactionRunner,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(
        claimId: ItemId,
        transition: ClaimTransition,
        answers: List<String> = emptyList(),
        note: String? = null
    ): Result<Claim> = runCatching {
        val user = getCurrentUser() ?: throw ClaimRequestException(ItemErrors.NO_SESSION)
        transaction.run {
            val claim = claims.getById(claimId) ?: throw ClaimRequestException(ClaimErrors.CLAIM_NOT_FOUND)
            if (transition.byStaff) {
                if (!user.role.isStaff) throw ClaimRequestException(ItemErrors.NOT_STAFF)
            } else if (claim.claimantId != user.id) {
                throw ClaimRequestException(ClaimErrors.NOT_YOUR_CLAIM)
            }
            val newStatus = ClaimStateMachine.apply(claim.status, transition)
            val now = clock.now()
            val others = claims.getByItem(claim.foundItemId).filter { it.id != claim.id }

            val updated = when (transition) {
                ClaimTransition.SUBMIT_ANSWERS -> {
                    val clean = answers.map { it.trim() }
                    if (clean.isEmpty() || clean.any { it.isEmpty() }) throw ClaimRequestException(ClaimErrors.ANSWERS_REQUIRED)
                    claim.copy(status = newStatus, controlAnswers = clean)
                }
                ClaimTransition.APPROVE -> {
                    if (others.any { it.status == ClaimStatus.APROBADO }) throw ClaimRequestException(ClaimErrors.ALREADY_APPROVED)
                    setMatchAndReport(claim, MatchStatus.CONFIRMADO, ReportStatus.MATCH_CONFIRMADO)
                    claim.copy(status = newStatus, reviewedBy = user.id, reviewedAt = now)
                }
                ClaimTransition.REJECT -> {
                    closeForClaimant(claim)
                    // Si nadie más lo está reclamando, el objeto vuelve al catálogo (RELEASE de H05).
                    val item = items.getById(claim.foundItemId)
                    if (item?.status == ItemStatus.RECLAMADO && others.none { ClaimStateMachine.isOpen(it.status) }) {
                        applyToItem(claim.foundItemId, ItemTransition.RELEASE)
                    }
                    claim.copy(status = newStatus, reviewedBy = user.id, reviewedAt = now)
                }
                ClaimTransition.DELIVER -> {
                    applyToItem(claim.foundItemId, ItemTransition.DELIVER)
                    setMatchAndReport(claim, matchStatus = null, reportStatus = ReportStatus.RECUPERADO)
                    // El objeto ya salió de custodia: los demás reclamos abiertos quedan rechazados.
                    others.filter { ClaimStateMachine.isOpen(it.status) }.forEach { other ->
                        closeForClaimant(other)
                        claims.update(other.copy(status = ClaimStatus.RECHAZADO, reviewedBy = user.id, reviewedAt = now))
                        audit.record(
                            AuditAction.CLAIM_REJECTED, user.id, other.id.value,
                            "Reclamo #${other.number} · el objeto se entregó con el reclamo #${claim.number}"
                        )
                    }
                    claim.copy(status = newStatus, deliveredBy = user.id, deliveredAt = now)
                }
            }
            claims.update(updated)
            audit.record(
                transition.auditAction, user.id, claim.id.value,
                listOfNotNull("Reclamo #${claim.number} · ${claim.status.label} → ${newStatus.label}", note?.trim()?.ifEmpty { null })
                    .joinToString(" · ")
            )
            updated
        }
    }

    /**
     * Coincidencia y reporte del reclamo, solo si el reporte es de quien reclama: un reclamo nunca cambia el
     * reporte de otra persona (aunque un dato mal cargado apunte a una coincidencia ajena).
     */
    private suspend fun ownMatch(claim: Claim): Pair<Match, LostReport>? {
        val match = claim.matchId?.let { claims.getMatch(it) } ?: return null
        val report = claims.getReport(match.lostReportId)?.takeIf { it.ownerId == claim.claimantId } ?: return null
        return match to report
    }

    private suspend fun setMatchAndReport(claim: Claim, matchStatus: MatchStatus?, reportStatus: ReportStatus) {
        val (match, report) = ownMatch(claim) ?: return
        matchStatus?.let { claims.updateMatchStatus(match.id, it) }
        claims.updateReportStatus(report.id, reportStatus)
    }

    /** Reclamo que no prosperó: su coincidencia se descarta y el reporte vuelve a buscar si no le queda otro reclamo. */
    private suspend fun closeForClaimant(claim: Claim) {
        val (match, report) = ownMatch(claim) ?: return
        claims.updateMatchStatus(match.id, MatchStatus.DESCARTADO)
        val reportHasOtherOpenClaim = claims.getByClaimant(claim.claimantId).any { other ->
            other.id != claim.id && ClaimStateMachine.isOpen(other.status) &&
                other.matchId?.let { claims.getMatch(it) }?.lostReportId == report.id
        }
        if (!reportHasOtherOpenClaim && report.status in IN_REVIEW) claims.updateReportStatus(report.id, ReportStatus.BUSCANDO)
    }

    private suspend fun applyToItem(itemId: ItemId, transition: ItemTransition) {
        val item = items.getById(itemId) ?: throw ClaimRequestException(ItemErrors.NOT_FOUND)
        items.updateStatus(itemId, ItemStateMachine.apply(item.status, transition))
    }

    private companion object {
        val IN_REVIEW = setOf(ReportStatus.EN_VERIFICACION, ReportStatus.MATCH_CONFIRMADO)
    }
}

/** PENDIENTE → EN_VERIFICACION: quien reclama responde las preguntas de control (pantalla en V01). */
class SubmitClaimAnswersUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, answers: List<String>): Result<Claim> =
        change(claimId, ClaimTransition.SUBMIT_ANSWERS, answers = answers)
}

/** EN_VERIFICACION → APROBADO: el staff valida las respuestas (V03). */
class ApproveClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, note: String? = null): Result<Claim> =
        change(claimId, ClaimTransition.APPROVE, note = note)
}

/** PENDIENTE / EN_VERIFICACION → RECHAZADO (V03). */
class RejectClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, note: String? = null): Result<Claim> =
        change(claimId, ClaimTransition.REJECT, note = note)
}

/** APROBADO → ENTREGADO: el dueño recibió el objeto (V04). */
class DeliverClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, note: String? = null): Result<Claim> =
        change(claimId, ClaimTransition.DELIVER, note = note)
}
