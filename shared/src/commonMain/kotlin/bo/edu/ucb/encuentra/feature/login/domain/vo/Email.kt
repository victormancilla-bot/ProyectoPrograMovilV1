package bo.edu.ucb.encuentra.feature.login.domain.vo

import kotlin.jvm.JvmInline

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email no puede estar vacío" }
    }
}
