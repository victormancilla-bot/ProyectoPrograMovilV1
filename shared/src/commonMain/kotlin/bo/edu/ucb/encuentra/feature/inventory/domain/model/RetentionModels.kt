package bo.edu.ucb.encuentra.feature.inventory.domain.model

import bo.edu.ucb.encuentra.core.domain.model.FoundItem

/** Lo que el staff decide con un objeto vencido (D04). Cada una es una transición de H05 y queda auditada. */
enum class RetentionAction(val label: String, val pastLabel: String) {
    DONATE("Donar", "donado"),
    DISCARD("Descartar", "descartado")
}

/** Pantalla de Retención: vencidos (para donar o descartar) y por vencer (solo aviso). */
data class RetentionOverview(
    val expired: List<FoundItem>,
    val expiring: List<FoundItem>
) {
    val isEmpty: Boolean get() = expired.isEmpty() && expiring.isEmpty()
}

/** Resultado del chequeo diario (WorkManager): cuántos hay y a cuántas personas del staff se avisó. */
data class RetentionCheck(
    val expired: Int,
    val expiring: Int,
    val notifiedStaff: Int
)

object RetentionErrors {
    const val NOT_STAFF = "Solo el staff puede decidir sobre objetos vencidos."
    const val NOT_FOUND = "El objeto no existe."
    const val NOT_EXPIRED = "Este objeto todavía no venció: solo se donan o descartan los vencidos."
}
