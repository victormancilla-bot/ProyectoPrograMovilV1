package org.example.project.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

fun initKoinAndroid(context: Context) {
    initKoin {
        androidLogger()
        androidContext(context)
    }
}
