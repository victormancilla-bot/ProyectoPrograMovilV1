package bo.edu.ucb.encuentra.core.scheduler

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.CheckRetentionUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * D04 · Chequeo diario de retención. WorkManager lo corre aunque la app esté cerrada: puede arrancar el proceso
 * sin pantalla, por eso primero recupera la sesión guardada (S01) para saber si quien usa el teléfono es staff y
 * mostrarle la notificación al momento; si no, el aviso queda en la bandeja del staff (N01/N04).
 */
class RetentionCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val session: SessionRepository by inject()
    private val checkRetention: CheckRetentionUseCase by inject()

    override suspend fun doWork(): Result = runCatching {
        session.restore()
        checkRetention()
    }.fold(
        onSuccess = { check ->
            Log.d(TAG, "Retención: ${check.expired} vencidos, ${check.expiring} por vencer, aviso a ${check.notifiedStaff} del staff")
            Result.success()
        },
        onFailure = { error ->
            Log.e(TAG, "Falló el chequeo de retención (intento ${runAttemptCount + 1})", error)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    )

    private companion object {
        const val TAG = "UcbRetention"
        const val MAX_ATTEMPTS = 3
    }
}
