package bo.edu.ucb.encuentra.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * "Por vencer" y "Vencido" no son estados guardados (H05): se calculan con la fecha de retención. Solo aplican a
 * objetos DISPONIBLES (uno reclamado espera su entrega; uno entregado, donado o descartado ya salió de custodia).
 */
enum class RetentionState(val label: String) {
    VIGENTE("Vigente"),
    POR_VENCER("Por vencer"),
    VENCIDO("Vencido")
}

/** Ventana de "por vencer": la misma que el contador del Dashboard (D01). */
val EXPIRING_WINDOW: Duration = 7.days

fun FoundItem.retentionState(now: Instant, window: Duration = EXPIRING_WINDOW): RetentionState = when {
    status != ItemStatus.DISPONIBLE -> RetentionState.VIGENTE
    retentionUntil < now -> RetentionState.VENCIDO
    retentionUntil <= now + window -> RetentionState.POR_VENCER
    else -> RetentionState.VIGENTE
}

/** Código corto para buscar y para la etiqueta del objeto ("#CALCULAD", "#3F9A2C1B"). */
val FoundItem.code: String get() = "#" + id.value.removePrefix("fi-").replace("-", "").take(CODE_LENGTH).uppercase()

private const val CODE_LENGTH = 8
