package bo.edu.ucb.encuentra.feature.login.domain.vo

import kotlin.jvm.JvmInline

@JvmInline
value class Password(val value: String) {
    init {
        require(value.isNotBlank()) { "Password no puede estar vacío" }
    }
}
