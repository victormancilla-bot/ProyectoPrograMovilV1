package bo.edu.ucb.encuentra.core.di

import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.local.SESSION_DATASTORE_FILE
import bo.edu.ucb.encuentra.core.data.local.createSessionDataStore
import bo.edu.ucb.encuentra.core.data.local.documentDirectory
import bo.edu.ucb.encuentra.core.data.local.getDatabaseBuilder
import bo.edu.ucb.encuentra.core.data.local.getRoomDatabase
import bo.edu.ucb.encuentra.core.data.IosLocalNotifier
import bo.edu.ucb.encuentra.core.domain.repository.LocalNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { createSessionDataStore { documentDirectory() + "/" + SESSION_DATASTORE_FILE } }
    single<LocalNotifier> { IosLocalNotifier() }
}
