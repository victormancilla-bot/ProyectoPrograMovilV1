package bo.edu.ucb.encuentra.feature.home.di

import bo.edu.ucb.encuentra.feature.home.data.repository.HomeActivityRepositoryImpl
import bo.edu.ucb.encuentra.feature.home.domain.repository.HomeActivityRepository
import bo.edu.ucb.encuentra.feature.home.domain.usecase.ObserveHomeActivityUseCase
import bo.edu.ucb.encuentra.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val homeModule = module {
    single<HomeActivityRepository> { HomeActivityRepositoryImpl(get(), get()) }
    singleOf(::ObserveHomeActivityUseCase)
    viewModelOf(::HomeViewModel)
}
