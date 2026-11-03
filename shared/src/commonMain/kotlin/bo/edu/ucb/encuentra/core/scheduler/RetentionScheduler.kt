package bo.edu.ucb.encuentra.core.scheduler

/**
 * D04 · Programa el chequeo de retención en segundo plano (patrón de la clase de WorkManager: expect/actual).
 * En Android [context] debe ser un `android.content.Context`; en iOS no se usa.
 */
expect class RetentionScheduler(context: Any?) {
    /** PeriodicWorkRequest diario. Si ya está programado, lo deja como está (no reinicia el reloj). */
    fun scheduleDaily()

    /** Una sola ejecución, para probar sin esperar un día. */
    fun runOnce()
}
