package bo.edu.ucb.encuentra.feature.login.di

import bo.edu.ucb.encuentra.feature.login.domain.usecase.AuthenticateUseCase
import bo.edu.ucb.encuentra.feature.login.presentation.viewmodel.LoginViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val loginModule = module {
    singleOf(::AuthenticateUseCase)
    viewModelOf(::LoginViewModel)
}
