package bo.edu.ucb.encuentra.feature.foundcatalog.di

import bo.edu.ucb.encuentra.feature.foundcatalog.data.FoundCatalogRepositoryImpl
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.repository.FoundCatalogRepository
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.GetHowItWorksUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.domain.usecase.ObserveFoundCatalogUseCase
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogViewModel
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val foundCatalogModule = module {
    // L07: catálogo limitado (Room) y "Cómo funciona" (textos con datos de Remote Config).
    single<FoundCatalogRepository> { FoundCatalogRepositoryImpl(get()) }
    singleOf(::ObserveFoundCatalogUseCase)
    singleOf(::GetHowItWorksUseCase)
    viewModelOf(::FoundCatalogViewModel)
    viewModelOf(::HowItWorksViewModel)
}
