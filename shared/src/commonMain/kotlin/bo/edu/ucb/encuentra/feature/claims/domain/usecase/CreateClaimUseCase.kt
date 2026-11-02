package bo.edu.ucb.encuentra.feature.claims.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.AuditAction
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemStateMachine
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.model.ItemTransition
import bo.edu.ucb.encuentra.core.domain.model.MatchStatus
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequestException
import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.core.domain.repository.DuplicateClaimException
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimErrors
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import kotlin.time.Clock

/**
 * "Esto es mío — verificar" (V02, desde el Detalle de match de M05). Todo en una transacción:
 * 1. Valida que la coincidencia sea del reporte de quien reclama y siga abierta, y que el objeto siga en custodia.
 * 2. Bloquea el doble reclamo del mismo objeto por la misma persona (también lo impide el índice único de Room).
 * 3. Crea el reclamo PENDIENTE (después responde las preguntas de control, V01).
 * 4. El objeto pasa a RECLAMADO con la máquina de estados de H05. Si ya estaba RECLAMADO por otra persona, el
 *    reclamo igual se crea: el dueño real no debe quedar bloqueado porque alguien reclamó antes. El staff decide.
 * 5. El reporte pasa a "En verificación" y se audita.
 */
class CreateClaimUseCase(
    private val claims: ClaimRepository,
    private val items: FoundItemLifecycleRepository,
    private val audit: AuditRecorder,
    private val transaction: TransactionRunner,
    private val clock: Clock
) : ClaimRequester {

    override suspend fun requestClaim(matchId: ItemId, claimantId: String): Int = transaction.run {
        val match = claims.getMatch(matchId) ?: throw ClaimRequestException(ClaimErrors.MATCH_NOT_FOUND)
        val report = claims.getReport(match.lostReportId) ?: throw ClaimRequestException(ClaimErrors.MATCH_NOT_FOUND)
        if (report.ownerId != claimantId) throw ClaimRequestException(ClaimErrors.NOT_YOUR_REPORT)
        if (match.status != MatchStatus.PROPUESTO || report.status !in OPEN_REPORTS) {
            throw ClaimRequestException(ClaimErrors.MATCH_CLOSED)
        }

        val item = items.getById(match.foundItemId) ?: throw ClaimRequestException(ClaimErrors.ITEM_UNAVAILABLE)
        if (!ItemStateMachine.isInCustody(item.status)) throw ClaimRequestException(ClaimErrors.ITEM_UNAVAILABLE)
        claims.getByItem(item.id).firstOrNull { it.claimantId == claimantId }?.let { throw DuplicateClaimException(it.number) }

        val number = claims.nextNumber()
        val claim = Claim(
            id = ItemId("c-$number"),
            number = number,
            foundItemId = item.id,
            matchId = match.id,
            claimantId = claimantId,
            status = ClaimStatus.PENDIENTE,
            controlAnswers = emptyList(),
            createdAt = clock.now(),
            reviewedBy = null,
            reviewedAt = null,
            scheduledDeliveryAt = null,
            deliveredBy = null,
            deliveredAt = null
        )
        claims.insert(claim)

        val itemChange = if (item.status == ItemStatus.DISPONIBLE) {
            val newStatus = ItemStateMachine.apply(item.status, ItemTransition.CLAIM)
            items.updateStatus(item.id, newStatus)
            " · objeto ${item.status.label} → ${newStatus.label}"
        } else {
            " · el objeto ya tenía otro reclamo"
        }
        if (report.status == ReportStatus.BUSCANDO) claims.updateReportStatus(report.id, ReportStatus.EN_VERIFICACION)

        audit.record(AuditAction.CLAIM_CREATED, claimantId, claim.id.value, "Reclamo #$number · ${item.title}$itemChange")
        number
    }

    private companion object {
        val OPEN_REPORTS = setOf(ReportStatus.BUSCANDO, ReportStatus.EN_VERIFICACION)
    }
}
