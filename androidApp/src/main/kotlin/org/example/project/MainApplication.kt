package org.example.project

import android.app.Application
import org.example.project.di.initKoinAndroid

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
