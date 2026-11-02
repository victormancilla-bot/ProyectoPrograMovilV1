package bo.edu.ucb.encuentra.core.domain.model

/**
 * Estado de un objeto ENCONTRADO en custodia (H05): DISPONIBLE → RECLAMADO → ENTREGADO / DESCARTADO / DONADO.
 * "Por vencer" y "Vencido" no son estados: se calculan con la fecha de retención (D04).
 * Las transiciones válidas están en [ItemStateMachine].
 */
enum class ItemStatus(val label: String) {
    DISPONIBLE("Disponible"),
    RECLAMADO("Reclamado"),
    ENTREGADO("Entregado"),
    DESCARTADO("Descartado"),
    DONADO("Donado")
}

/** Estado de un reporte de objeto PERDIDO (Mis Reportes, L05: chips Buscando / Verificación). */
enum class ReportStatus(val label: String) {
    BUSCANDO("Buscando"),
    EN_VERIFICACION("En verificación"),
    MATCH_CONFIRMADO("Match confirmado"),
    RECUPERADO("Recuperado"),
    CERRADO("Cerrado"),
    VENCIDO("Vencido")
}

/** Estado de un reclamo (V02): PENDIENTE → EN_VERIFICACION → APROBADO / RECHAZADO → ENTREGADO. */
enum class ClaimStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_VERIFICACION("En verificación"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado"),
    ENTREGADO("Entregado")
}

/** Estado de una coincidencia (M02): "No es mío" la DESCARTA y no vuelve a aparecer. */
enum class MatchStatus(val label: String) {
    PROPUESTO("Propuesto"),
    CONFIRMADO("Confirmado"),
    DESCARTADO("Descartado")
}

/** Dónde se guarda físicamente un objeto encontrado (historial de custodia, H05). */
enum class CustodyPlace(val label: String) {
    OFICINA("Oficina"),
    LOCKER("Locker"),
    BODEGA("Bodega")
}

/** Eventos del registro de auditoría append-only (S05). */
enum class AuditAction(val label: String) {
    ITEM_REGISTERED("Objeto registrado"),
    CUSTODY_CHANGED("Custodia cambiada"),
    CLAIM_CREATED("Reclamo creado"),
    CLAIM_ANSWERED("Preguntas respondidas"),
    CLAIM_APPROVED("Reclamo aprobado"),
    CLAIM_REJECTED("Reclamo rechazado"),
    ITEM_DELIVERED("Objeto entregado"),
    ITEM_DISCARDED("Objeto descartado"),
    ITEM_DONATED("Objeto donado"),
    ROLE_CHANGED("Rol cambiado"),
    REPORT_CLOSED("Reporte cerrado"),
    DELIVERY_SCHEDULED("Entrega programada")
}

/** Tipo de notificación interna = canal de N01 (coincidencias, reclamos, retención). */
enum class NotificationType(val label: String) {
    NEW_MATCH("Coincidencias"),
    CLAIM_UPDATE("Reclamos"),
    RETENTION("Retención"),
    GENERAL("General")
}
