package bo.edu.ucb.encuentra.feature.alerts.di

import bo.edu.ucb.encuentra.feature.alerts.presentation.viewmodel.AlertsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val alertsModule = module {
    viewModelOf(::AlertsViewModel)
}
