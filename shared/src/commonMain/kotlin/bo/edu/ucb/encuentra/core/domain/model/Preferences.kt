package bo.edu.ucb.encuentra.core.domain.model

/** Tema de la app (S07). SISTEMA sigue el modo oscuro del teléfono. */
enum class ThemeMode(val label: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/** Idioma elegido (S07). Los textos en inglés llegan con F09 (Localise). */
enum class AppLanguage(val tag: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English")
}

/** Preferencias del teléfono: valen antes de iniciar sesión y para cualquier usuario. */
data class DevicePreferences(
    val theme: ThemeMode = ThemeMode.SISTEMA,
    val language: AppLanguage = AppLanguage.ES
)

/**
 * Preferencias de cada usuario (en la demo varios comparten el teléfono): qué avisos ver en el sistema y el
 * WhatsApp de contacto que se prellena en los reportes. Por defecto todos los avisos están activos.
 */
data class UserPreferences(
    val disabledNotifications: Set<NotificationType> = emptySet(),
    val contactPhone: String = ""
) {
    fun notifies(type: NotificationType): Boolean = type !in disabledNotifications
}

/** Celular boliviano: 8 dígitos que empiezan en 2-7, con +591 opcional (la misma regla del wizard, L03). */
object BolivianPhone {
    private val REGEX = Regex("""^(\+?591)?[2-7]\d{7}$""")

    fun normalize(raw: String): String = raw.filterNot { it == ' ' || it == '-' }

    fun isValid(raw: String): Boolean = REGEX.matches(normalize(raw))
}
