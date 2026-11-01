package bo.edu.ucb.encuentra.feature.home.di

import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
