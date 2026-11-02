package bo.edu.ucb.encuentra.core.domain.vo

import kotlin.jvm.JvmInline

@JvmInline
value class Password(val value: String) {
    init {
        require(value.isNotBlank()) { "Password no puede estar vacío" }
    }

    fun isValid(): Boolean = value.length >= MIN_LENGTH

    companion object {
        const val MIN_LENGTH = 6
    }
}
