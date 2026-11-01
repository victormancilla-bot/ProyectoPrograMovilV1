package bo.edu.ucb.encuentra

import android.app.Application
import bo.edu.ucb.encuentra.di.initKoinAndroid

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
