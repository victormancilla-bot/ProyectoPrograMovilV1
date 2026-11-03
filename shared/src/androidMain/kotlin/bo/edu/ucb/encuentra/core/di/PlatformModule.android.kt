package bo.edu.ucb.encuentra.core.di

import android.content.Context
import bo.edu.ucb.encuentra.core.data.AndroidConnectivityMonitor
import bo.edu.ucb.encuentra.core.data.AndroidLocalNotifier
import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.local.SESSION_DATASTORE_FILE
import bo.edu.ucb.encuentra.core.data.local.createSessionDataStore
import bo.edu.ucb.encuentra.core.data.local.getDatabaseBuilder
import bo.edu.ucb.encuentra.core.data.local.getRoomDatabase
import bo.edu.ucb.encuentra.core.domain.repository.ConnectivityMonitor
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder(get())) }
    single { createSessionDataStore { get<Context>().filesDir.resolve(SESSION_DATASTORE_FILE).absolutePath } }
    // N01: notificaciones del sistema con canales.
    single<LocalNotifier> { AndroidLocalNotifier(get()) }
    // F11: banner sin conexión.
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(get()) }
}
