package bo.edu.ucb.encuentra.feature.claims.data

import bo.edu.ucb.encuentra.core.domain.model.AppNotification
import bo.edu.ucb.encuentra.core.domain.model.Claim
import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.core.domain.model.NotificationType
import bo.edu.ucb.encuentra.core.domain.repository.ClaimNotifier
import bo.edu.ucb.encuentra.core.domain.usecase.NotificationCenter
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.core.presentation.formatHm
import bo.edu.ucb.encuentra.core.presentation.formatShort
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Implementación del hook de N01: una notificación (canal "Reclamos") para quien reclamó, publicada en el
 * [NotificationCenter] (Room + sistema).
 * El deep link abre el Detalle de match, donde el estudiante ve en qué quedó su reclamo (F05).
 */
class RoomClaimNotifier(
    /** N01: guarda en Room y avisa en el sistema (si quien reclamó es quien usa la app). */
    private val notifications: NotificationCenter,
    private val clock: Clock,
    private val timeZone: TimeZone
) : ClaimNotifier {

    override suspend fun onDeliveryScheduled(claim: Claim, itemTitle: String) {
        val at = claim.scheduledDeliveryAt ?: return
        val local = at.toLocalDateTime(timeZone)
        notifications.publish(
            AppNotification(
                // Una por reclamo: si se reprograma, se reemplaza y vuelve a quedar sin leer.
                id = ItemId("n-claim-${claim.id.value}-entrega"),
                userId = claim.claimantId,
                type = NotificationType.CLAIM_UPDATE,
                title = "Entrega programada",
                body = "Recoge \"$itemTitle\" el ${local.date.formatShort()} a las ${local.time.formatHm()} en Objetos " +
                    "Perdidos. Lleva tu carnet universitario.",
                deepLink = claim.matchId?.let { "ucbencuentra://match/${it.value}" },
                read = false,
                createdAt = clock.now()
            )
        )
    }

    override suspend fun onClaimUpdated(claim: Claim, itemTitle: String) {
        val (title, body) = messageFor(claim, itemTitle) ?: return
        notifications.publish(
            AppNotification(
                // Una por reclamo y estado: repetir la llamada no duplica la notificación.
                id = ItemId("n-claim-${claim.id.value}-${claim.status.name.lowercase()}"),
                userId = claim.claimantId,
                type = NotificationType.CLAIM_UPDATE,
                title = title,
                body = body,
                deepLink = claim.matchId?.let { "ucbencuentra://match/${it.value}" },
                read = false,
                createdAt = clock.now()
            )
        )
    }

    companion object {
        /** Texto de la notificación según el nuevo estado; null si ese estado no se avisa. */
        fun messageFor(claim: Claim, itemTitle: String): Pair<String, String>? = when (claim.status) {
            ClaimStatus.APROBADO -> "Reclamo #${claim.number} aprobado" to
                "Tu reclamo de \"$itemTitle\" fue aprobado. Acércate a Objetos Perdidos con tu carnet para recogerlo."
            ClaimStatus.RECHAZADO -> "Reclamo #${claim.number} rechazado" to
                if (claim.attemptsLeft == 0) {
                    "Se agotaron los intentos para responder las preguntas de \"$itemTitle\"."
                } else {
                    "El staff no pudo confirmar que \"$itemTitle\" sea tuyo."
                }
            ClaimStatus.ENTREGADO -> "Objeto entregado" to "Registramos la entrega de \"$itemTitle\". ¡Gracias por usar UCB Encuentra!"
            ClaimStatus.PENDIENTE, ClaimStatus.EN_VERIFICACION -> null
        }
    }
}
