package bo.edu.ucb.encuentra.feature.alerts.di

import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsViewModel
import bo.edu.ucb.encuentra.feature.alerts.data.AlertsRepositoryImpl
import bo.edu.ucb.encuentra.feature.alerts.domain.repository.AlertsRepository
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ClearReadAlertsUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAlertReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.MarkAllAlertsReadUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveAlertsUseCase
import bo.edu.ucb.encuentra.feature.alerts.domain.usecase.ObserveUnreadAlertsCountUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val alertsModule = module {
    single<AlertsRepository> { AlertsRepositoryImpl(get()) }
    singleOf(::ObserveAlertsUseCase)
    singleOf(::ObserveUnreadAlertsCountUseCase)
    singleOf(::MarkAlertReadUseCase)
    singleOf(::MarkAllAlertsReadUseCase)
    singleOf(::ClearReadAlertsUseCase)
    viewModelOf(::AlertsViewModel)
}
