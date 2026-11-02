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
import bo.edu.ucb.encuentra.core.domain.repository.ClaimNotifier
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ItemErrors
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerComparator
import bo.edu.ucb.encuentra.feature.claims.domain.model.AnswerVerdict
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
    private val clock: Clock,
    /** Hook de N01: avisa a quien reclamó cuando su reclamo se aprueba, rechaza, bloquea o entrega. */
    private val notifier: ClaimNotifier = NoClaimNotifications
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
            // V03: aprobar o rechazar exige un motivo, que queda en la auditoría (S05).
            if (transition in DECISIONS && note.isNullOrBlank()) throw ClaimRequestException(ClaimErrors.REASON_REQUIRED)
            val newStatus = ClaimStateMachine.apply(claim.status, transition)
            val now = clock.now()
            val others = claims.getByItem(claim.foundItemId).filter { it.id != claim.id }
            // Responder no siempre avanza: puede sumar un intento fallido o bloquear el reclamo (V01).
            if (transition == ClaimTransition.SUBMIT_ANSWERS) return@run submitAnswers(claim, answers, user.id, others)

            val updated = when (transition) {
                ClaimTransition.SUBMIT_ANSWERS -> error("Se maneja en submitAnswers")
                ClaimTransition.APPROVE -> {
                    if (others.any { it.status == ClaimStatus.APROBADO }) throw ClaimRequestException(ClaimErrors.ALREADY_APPROVED)
                    setMatchAndReport(claim, MatchStatus.CONFIRMADO, ReportStatus.MATCH_CONFIRMADO)
                    claim.copy(status = newStatus, reviewedBy = user.id, reviewedAt = now)
                }
                ClaimTransition.REJECT -> {
                    closeForClaimant(claim)
                    releaseIfNobodyElse(claim, others)
                    claim.copy(status = newStatus, reviewedBy = user.id, reviewedAt = now)
                }
                ClaimTransition.DELIVER -> {
                    applyToItem(claim.foundItemId, ItemTransition.DELIVER)
                    setMatchAndReport(claim, matchStatus = null, reportStatus = ReportStatus.RECUPERADO)
                    // El objeto ya salió de custodia: los demás reclamos abiertos quedan rechazados.
                    others.filter { ClaimStateMachine.isOpen(it.status) }.forEach { other ->
                        closeForClaimant(other)
                        val rejected = other.copy(status = ClaimStatus.RECHAZADO, reviewedBy = user.id, reviewedAt = now)
                        claims.update(rejected)
                        audit.record(
                            AuditAction.CLAIM_REJECTED, user.id, other.id.value,
                            "Reclamo #${other.number} · el objeto se entregó con el reclamo #${claim.number}"
                        )
                        notify(rejected)
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
            notify(updated)
            updated
        }
    }

    private suspend fun notify(claim: Claim) {
        notifier.onClaimUpdated(claim, items.getById(claim.foundItemId)?.title ?: "tu objeto")
    }

    /**
     * V01: compara cada respuesta con la esperada ([AnswerComparator]).
     * - Todas coinciden (o el objeto no tiene respuesta esperada) → EN_VERIFICACION: decide el staff.
     * - Alguna no coincide → sigue PENDIENTE con un intento fallido más. No se dice cuál falló ni cuál era la
     *   respuesta, para no regalar pistas.
     * - Al llegar a [Claim.MAX_ANSWER_ATTEMPTS] → el reclamo se bloquea (RECHAZADO) y se audita.
     * Las respuestas se guardan siempre: son privadas y solo las ve el staff.
     */
    private suspend fun submitAnswers(claim: Claim, answers: List<String>, actorId: String, others: List<Claim>): Claim {
        val item = items.getById(claim.foundItemId) ?: throw ClaimRequestException(ItemErrors.NOT_FOUND)
        val clean = answers.map { it.trim() }
        if (clean.size != item.controlQuestions.size || clean.any { it.isEmpty() }) {
            throw ClaimRequestException(ClaimErrors.ANSWERS_REQUIRED)
        }
        val verdicts = clean.mapIndexed { i, answer -> AnswerComparator.compare(answer, item.expectedAnswers.getOrNull(i)) }
        val prefix = "Reclamo #${claim.number}"

        if (verdicts.none { it == AnswerVerdict.NO_COINCIDE }) {
            val answered = claim.copy(status = ClaimStatus.EN_VERIFICACION, controlAnswers = clean)
            claims.update(answered)
            audit.record(
                AuditAction.CLAIM_ANSWERED, actorId, claim.id.value,
                "$prefix · ${claim.status.label} → ${answered.status.label} · ${clean.size} respuesta(s)"
            )
            return answered
        }

        val attempts = claim.failedAttempts + 1
        if (attempts < Claim.MAX_ANSWER_ATTEMPTS) {
            val retry = claim.copy(controlAnswers = clean, failedAttempts = attempts)
            claims.update(retry)
            audit.record(
                AuditAction.CLAIM_ANSWERED, actorId, claim.id.value,
                "$prefix · respuestas no coinciden (intento $attempts de ${Claim.MAX_ANSWER_ATTEMPTS})"
            )
            return retry
        }

        closeForClaimant(claim)
        releaseIfNobodyElse(claim, others)
        val blocked = claim.copy(
            status = ClaimStatus.RECHAZADO, controlAnswers = clean, failedAttempts = attempts, reviewedAt = clock.now()
        )
        claims.update(blocked)
        audit.record(
            AuditAction.CLAIM_REJECTED, actorId, claim.id.value,
            "$prefix · bloqueado: ${Claim.MAX_ANSWER_ATTEMPTS} intentos sin coincidir"
        )
        notify(blocked)
        return blocked
    }

    /** Si nadie más lo está reclamando, el objeto vuelve al catálogo (RELEASE de H05). */
    private suspend fun releaseIfNobodyElse(claim: Claim, others: List<Claim>) {
        val item = items.getById(claim.foundItemId)
        if (item?.status == ItemStatus.RECLAMADO && others.none { ClaimStateMachine.isOpen(it.status) }) {
            applyToItem(claim.foundItemId, ItemTransition.RELEASE)
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
        val DECISIONS = setOf(ClaimTransition.APPROVE, ClaimTransition.REJECT)
        val IN_REVIEW = setOf(ReportStatus.EN_VERIFICACION, ReportStatus.MATCH_CONFIRMADO)
    }
}

/**
 * Quien reclama responde las preguntas de control (V01). Devuelve el reclamo: EN_VERIFICACION si coincidieron,
 * PENDIENTE con un intento fallido más si no, o RECHAZADO si se acabaron los intentos.
 */
class SubmitClaimAnswersUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, answers: List<String>): Result<Claim> =
        change(claimId, ClaimTransition.SUBMIT_ANSWERS, answers = answers)
}

/** EN_VERIFICACION → APROBADO: el staff valida las respuestas (V03). */
class ApproveClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    /** [reason] es obligatorio (V03): queda en la auditoría. */
    suspend operator fun invoke(claimId: ItemId, reason: String): Result<Claim> =
        change(claimId, ClaimTransition.APPROVE, note = reason)
}

/** PENDIENTE / EN_VERIFICACION → RECHAZADO (V03). */
class RejectClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    /** [reason] es obligatorio (V03): queda en la auditoría. */
    suspend operator fun invoke(claimId: ItemId, reason: String): Result<Claim> =
        change(claimId, ClaimTransition.REJECT, note = reason)
}

/** APROBADO → ENTREGADO: el dueño recibió el objeto (V04). */
class DeliverClaimUseCase(private val change: ChangeClaimStatusUseCase) {
    suspend operator fun invoke(claimId: ItemId, note: String? = null): Result<Claim> =
        change(claimId, ClaimTransition.DELIVER, note = note)
}

/** Sin notificaciones (tests y valor por defecto); Koin inyecta [bo.edu.ucb.encuentra.feature.claims.data.RoomClaimNotifier]. */
object NoClaimNotifications : ClaimNotifier {
    override suspend fun onClaimUpdated(claim: Claim, itemTitle: String) = Unit
}
