package bo.edu.ucb.encuentra.core.scheduler

/**
 * iOS: pendiente (WorkManager es solo de Android; en iOS sería BGTaskScheduler). Existe para que compile.
 * El staff igual ve vencidos y por vencer en la pantalla de Retención.
 */
actual class RetentionScheduler actual constructor(context: Any?) {
    actual fun scheduleDaily() = Unit

    actual fun runOnce() = Unit
}
