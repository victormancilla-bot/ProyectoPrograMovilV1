package bo.edu.ucb.encuentra.core.domain.vo

import kotlin.jvm.JvmInline

/**
 * VO (Value Object): un correo no es un String cualquiera. No puede estar vacío y la app solo acepta
 * cuentas institucionales (@ucb.edu.bo): ver [isInstitutional].
 */
@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email no puede estar vacío" }
    }

    /** true si es una cuenta @ucb.edu.bo con algo antes de la @ (S01 rechaza las demás). */
    fun isInstitutional(): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.endsWith("@$INSTITUTIONAL_DOMAIN") && normalized.substringBefore("@").isNotBlank()
    }

    companion object {
        const val INSTITUTIONAL_DOMAIN = "ucb.edu.bo"
    }
}
