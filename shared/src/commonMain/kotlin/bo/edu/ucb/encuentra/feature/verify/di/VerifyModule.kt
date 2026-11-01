package bo.edu.ucb.encuentra.feature.verify.di

import bo.edu.ucb.encuentra.feature.verify.presentation.viewmodel.VerifyViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val verifyModule = module {
    viewModelOf(::VerifyViewModel)
}
