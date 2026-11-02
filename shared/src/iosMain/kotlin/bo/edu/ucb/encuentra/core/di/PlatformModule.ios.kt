package bo.edu.ucb.encuentra.core.di

import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.local.getDatabaseBuilder
import bo.edu.ucb.encuentra.core.data.local.getRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
}
