package bo.edu.ucb.encuentra.feature.myreports.di

import bo.edu.ucb.encuentra.feature.myreports.data.MyReportsRepositoryImpl
import bo.edu.ucb.encuentra.feature.myreports.domain.repository.MyReportsRepository
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.CloseReportUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveMyReportsUseCase
import bo.edu.ucb.encuentra.feature.myreports.domain.usecase.ObserveReportDetailUseCase
import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Mis reportes (L05): lista con filtros, detalle con coincidencias y cerrar reporte.
val myReportsModule = module {
    single<MyReportsRepository> { MyReportsRepositoryImpl(get(), get(), get()) }
    singleOf(::ObserveMyReportsUseCase)
    singleOf(::ObserveReportDetailUseCase)
    singleOf(::CloseReportUseCase)
    viewModelOf(::MyReportsViewModel)
}
