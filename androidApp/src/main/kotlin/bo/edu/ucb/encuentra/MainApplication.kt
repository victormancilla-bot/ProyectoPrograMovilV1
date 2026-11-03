package bo.edu.ucb.encuentra

import android.app.Application
import bo.edu.ucb.encuentra.core.scheduler.RetentionScheduler
import bo.edu.ucb.encuentra.di.initKoinAndroid

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)

        // D04: chequeo diario de retención (WorkManager). Para probar sin esperar un día:
        // RetentionScheduler(this).runOnce()
        RetentionScheduler(this).scheduleDaily()
    }
}
