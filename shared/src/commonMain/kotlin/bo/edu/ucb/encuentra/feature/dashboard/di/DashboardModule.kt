package bo.edu.ucb.encuentra.feature.dashboard.di

import bo.edu.ucb.encuentra.feature.dashboard.data.DashboardRepositoryImpl
import bo.edu.ucb.encuentra.feature.dashboard.data.RealtimeStatsDataSource
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.DashboardRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveDashboardStatsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveLatestPendingClaimsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val dashboardModule = module {
    // D01: contadores desde Realtime Database (stats/) con Room como respaldo en vivo.
    singleOf(::RealtimeStatsDataSource)
    single<DashboardRepository> { DashboardRepositoryImpl(get(), get(), get(), get()) }
    single { ObserveDashboardStatsUseCase(get(), get<kotlin.time.Clock>()) }
    singleOf(::ObserveLatestPendingClaimsUseCase)
    viewModelOf(::DashboardViewModel)
}
