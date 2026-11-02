package bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel

import bo.edu.ucb.encuentra.core.domain.model.ClaimStatus
import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.PhysicalChecks
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Confirmación de Entrega (V04, mockup "Confirmación de Entrega"). */
data class DeliveryState(
    val loading: Boolean = true,
    val error: String? = null,
    val review: ClaimReview? = null,
    /** Fecha y hora elegidas para programar (se prellenan con las ya programadas). */
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val checks: PhysicalChecks = PhysicalChecks(),
    val working: Boolean = false,
    val actionError: String? = null,
    /** Mensaje breve tras programar ("Entrega programada"). */
    val info: String? = null
) {
    val readyForDelivery: Boolean get() = review?.claim?.status == ClaimStatus.APROBADO
    val delivered: Boolean get() = review?.claim?.status == ClaimStatus.ENTREGADO
    val canSchedule: Boolean get() = readyForDelivery && date != null && time != null && !working
    val canConfirm: Boolean get() = readyForDelivery && checks.complete && !working
}

sealed class DeliveryEvent {
    data object OnBack : DeliveryEvent()
    data object OnRetry : DeliveryEvent()
    data class OnDatePicked(val date: LocalDate) : DeliveryEvent()
    data class OnTimePicked(val time: LocalTime) : DeliveryEvent()
    data object OnSchedule : DeliveryEvent()
    data class OnIdentityVerified(val checked: Boolean) : DeliveryEvent()
    data class OnItemReceived(val checked: Boolean) : DeliveryEvent()
    data object OnConfirm : DeliveryEvent()
}

sealed class DeliveryEffect {
    data object NavigateBack : DeliveryEffect()
}
