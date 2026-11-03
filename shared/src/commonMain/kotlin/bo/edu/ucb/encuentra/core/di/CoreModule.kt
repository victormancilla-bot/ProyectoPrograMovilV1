package bo.edu.ucb.encuentra.core.di

import bo.edu.ucb.encuentra.core.data.FoundItemLifecycleRepositoryImpl
import bo.edu.ucb.encuentra.core.data.RoomAuditRecorder
import bo.edu.ucb.encuentra.core.data.RoomTransactionRunner
import bo.edu.ucb.encuentra.core.data.SessionRepositoryImpl
import bo.edu.ucb.encuentra.core.data.UserRepositoryImpl
import bo.edu.ucb.encuentra.core.data.local.AppDatabase
import bo.edu.ucb.encuentra.core.data.remoteconfig.AppConfigRepositoryImpl
import bo.edu.ucb.encuentra.core.data.remoteconfig.FirebaseRemoteConfigDataSource
import bo.edu.ucb.encuentra.core.data.remoteconfig.RemoteConfigDataSource
import bo.edu.ucb.encuentra.core.domain.repository.AppConfigRepository
import bo.edu.ucb.encuentra.core.domain.repository.CapturedPhotosStore
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.ObserveAppConfigUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.RefreshAppConfigUseCase
import bo.edu.ucb.encuentra.core.data.local.seed.DatabaseSeeder
import bo.edu.ucb.encuentra.core.domain.repository.AuditRecorder
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.repository.TransactionRunner
import bo.edu.ucb.encuentra.core.domain.repository.UserRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.RestoreSessionUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ChangeItemStatusUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ClaimItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DeliverItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DiscardItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.DonateItemUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.MoveCustodyUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ObserveCustodyHistoryUseCase
import bo.edu.ucb.encuentra.core.domain.usecase.item.ReleaseItemUseCase
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Clock

// Lo que comparten todas las features: sesión persistente del usuario y los DAOs de Room (AppDatabase viene de platformModule).
val coreModule = module {
    single<SessionRepository> { SessionRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    singleOf(::GetCurrentUserUseCase)
    singleOf(::RestoreSessionUseCase)

    // Configuración remota (F08): un solo data source de Remote Config para toda la app (también el matching).
    single<RemoteConfigDataSource> { FirebaseRemoteConfigDataSource() }
    single<AppConfigRepository> { AppConfigRepositoryImpl(get()) }
    singleOf(::GetAppConfigUseCase)
    singleOf(::ObserveAppConfigUseCase)
    singleOf(::RefreshAppConfigUseCase)

    // Fotos del objeto que se está registrando (H01 → H03).
    singleOf(::CapturedPhotosStore)

    // Reloj y zona horaria inyectables: los tests usan un reloj fijo.
    single<Clock> { Clock.System }
    single { TimeZone.currentSystemDefault() }

    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().lostReportDao() }
    single { get<AppDatabase>().foundItemDao() }
    single { get<AppDatabase>().custodyEventDao() }
    single { get<AppDatabase>().matchDao() }
    single { get<AppDatabase>().claimDao() }
    single { get<AppDatabase>().auditLogDao() }
    single { get<AppDatabase>().appNotificationDao() }
    singleOf(::DatabaseSeeder)

    // Ciclo de vida del objeto encontrado (H05): máquina de estados, custodia y hook de auditoría (S05).
    single<TransactionRunner> { RoomTransactionRunner(get()) }
    single<AuditRecorder> { RoomAuditRecorder(get(), get()) }
    single<FoundItemLifecycleRepository> { FoundItemLifecycleRepositoryImpl(get(), get()) }
    singleOf(::ChangeItemStatusUseCase)
    singleOf(::ClaimItemUseCase)
    singleOf(::ReleaseItemUseCase)
    singleOf(::DeliverItemUseCase)
    singleOf(::DiscardItemUseCase)
    singleOf(::DonateItemUseCase)
    singleOf(::MoveCustodyUseCase)
    singleOf(::ObserveCustodyHistoryUseCase)
}
