package bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.claims.domain.model.ClaimReview
import kotlin.time.Instant

/** Decisión del staff pendiente de confirmar (no se puede deshacer). */
enum class ClaimDecision { APPROVE, REJECT }

/** Detalle de un reclamo para el staff (V01): respuestas, esperadas, sugerencia y decisión humana. */
data class ClaimDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val review: ClaimReview? = null,
    val confirming: ClaimDecision? = null,
    /** Motivo OBLIGATORIO de la decisión (V03): queda en la auditoría. */
    val note: String = "",
    /** Se intentó confirmar sin motivo. */
    val reasonMissing: Boolean = false,
    val working: Boolean = false,
    val actionError: String? = null,
    /** Para "Hace 2 horas". */
    val now: Instant? = null
) {
    val notFound: Boolean get() = !loading && error == null && review == null
    val canConfirm: Boolean get() = note.isNotBlank() && !working
}

sealed class ClaimDetailEvent {
    data object OnBack : ClaimDetailEvent()
    data object OnRetry : ClaimDetailEvent()
    data class OnDecide(val decision: ClaimDecision) : ClaimDetailEvent()
    data class OnNoteChanged(val note: String) : ClaimDetailEvent()
    data object OnConfirm : ClaimDetailEvent()
    data object OnDismiss : ClaimDetailEvent()
}

sealed class ClaimDetailEffect {
    data object NavigateBack : ClaimDetailEffect()
}
