package bo.edu.ucb.encuentra.feature.profile.di

import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante E. Aquí van los repositorios, use cases y ViewModels de esta feature.
val profileModule = module {
    viewModelOf(::ProfileViewModel)
}
