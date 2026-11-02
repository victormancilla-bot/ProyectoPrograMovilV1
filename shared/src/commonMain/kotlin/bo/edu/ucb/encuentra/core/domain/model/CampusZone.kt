package bo.edu.ucb.encuentra.core.domain.model

/**
 * Zona del campus donde se perdió o encontró un objeto (dropdown "¿Dónde lo perdiste?" de L03).
 * El detalle (aula, piso) va aparte como texto libre. M01 filtra coincidencias por zona.
 */
enum class CampusZone(val label: String) {
    BIBLIOTECA_CENTRAL("Biblioteca Central"),
    BLOQUE_A("Bloque A"),
    BLOQUE_B("Bloque B"),
    BLOQUE_C("Bloque C"),
    BLOQUE_D("Bloque D"),
    CAFETERIA("Cafetería Principal"),
    CANCHAS_DEPORTIVAS("Canchas Deportivas"),
    AUDITORIO("Auditorio"),
    LABORATORIOS("Laboratorios"),
    PARQUEO("Parqueo"),
    ADMINISTRACION("Administración"),
    OTRO("Otro lugar")
}
