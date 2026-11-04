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
import bo.edu.ucb.encuentra.feature.dashboard.data.ExportRepositoryImpl
import bo.edu.ucb.encuentra.feature.dashboard.data.MetricsRepositoryImpl
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.ExportRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.BuildExportUseCase
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveExportDataUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.ExportViewModel
import bo.edu.ucb.encuentra.feature.dashboard.domain.repository.MetricsRepository
import bo.edu.ucb.encuentra.feature.dashboard.domain.usecase.ObserveMetricsUseCase
import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.MetricsViewModel

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val dashboardModule = module {
    // D01: contadores desde Realtime Database (stats/) con Room como respaldo en vivo.
    singleOf(::RealtimeStatsDataSource)
    single<DashboardRepository> { DashboardRepositoryImpl(get(), get(), get(), get()) }
    single { ObserveDashboardStatsUseCase(get(), get<kotlin.time.Clock>()) }
    singleOf(::ObserveLatestPendingClaimsUseCase)
    viewModelOf(::DashboardViewModel)

    // D02: estadísticas (retención y recuperación por categoría).
    single<MetricsRepository> { MetricsRepositoryImpl(get(), get(), get()) }
    singleOf(::ObserveMetricsUseCase)
    viewModelOf(::MetricsViewModel)

    // D03: exportar a CSV (la auditoría legible viene de AuditTrailRepository, auditModule).
    single<ExportRepository> { ExportRepositoryImpl(get(), get(), get()) }
    singleOf(::ObserveExportDataUseCase)
    singleOf(::BuildExportUseCase)
    viewModelOf(::ExportViewModel)
}
