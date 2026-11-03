package bo.edu.ucb.encuentra.core.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

actual class RetentionScheduler actual constructor(context: Any?) {

    private val androidContext = (context as? Context)?.applicationContext
        ?: throw IllegalArgumentException("Context must be an instance of android.content.Context")

    // Los datos están en el teléfono (Room): no hace falta internet. Solo se evita correr con batería baja.
    private val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    actual fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<RetentionCheckWorker>(INTERVAL_DAYS, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(androidContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    actual fun runOnce() {
        val request = OneTimeWorkRequestBuilder<RetentionCheckWorker>().build()
        WorkManager.getInstance(androidContext)
            .enqueueUniqueWork(WORK_NAME_ONCE, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "retentionDailyCheck"
        const val WORK_NAME_ONCE = "retentionCheckOnce"
        const val INTERVAL_DAYS = 1L
    }
}
