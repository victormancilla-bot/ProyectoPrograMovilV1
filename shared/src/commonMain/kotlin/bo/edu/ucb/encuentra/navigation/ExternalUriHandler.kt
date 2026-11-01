package bo.edu.ucb.encuentra.navigation

/**
 * Puente entre la plataforma y la navegación para deep links que llegan con la app abierta
 * (Android: MainActivity.onNewIntent; iOS: onOpenURL). Si llega antes de que exista el NavHost, se guarda.
 */
object ExternalUriHandler {
    private var cached: String? = null

    var listener: ((String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let(value)
                cached = null
            }
        }

    fun onNewUri(uri: String) {
        val current = listener
        if (current != null) current(uri) else cached = uri
    }
}
