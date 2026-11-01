package bo.edu.ucb.encuentra.feature.dashboard.di

import bo.edu.ucb.encuentra.feature.dashboard.presentation.viewmodel.DashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val dashboardModule = module {
    viewModelOf(::DashboardViewModel)
}
