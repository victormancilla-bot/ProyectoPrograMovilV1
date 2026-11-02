package bo.edu.ucb.encuentra.core.di

import bo.edu.ucb.encuentra.core.data.SessionRepositoryImpl
import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.local.seed.DatabaseSeeder
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// Lo que comparten todas las features: sesión persistente del usuario y los DAOs de Room (AppDatabase viene de platformModule).
val coreModule = module {
    single<SessionRepository> { SessionRepositoryImpl(get(), get()) }
    singleOf(::GetCurrentUserUseCase)
    singleOf(::RestoreSessionUseCase)

    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().lostReportDao() }
    single { get<AppDatabase>().foundItemDao() }
    single { get<AppDatabase>().custodyEventDao() }
    single { get<AppDatabase>().matchDao() }
    single { get<AppDatabase>().claimDao() }
    single { get<AppDatabase>().auditLogDao() }
    single { get<AppDatabase>().appNotificationDao() }
    singleOf(::DatabaseSeeder)
}
