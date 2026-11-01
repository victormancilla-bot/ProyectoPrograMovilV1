package bo.edu.ucb.encuentra.feature.foundcatalog.di

import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.FoundCatalogViewModel
import bo.edu.ucb.encuentra.feature.foundcatalog.presentation.viewmodel.HowItWorksViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val foundCatalogModule = module {
    viewModelOf(::FoundCatalogViewModel)
    viewModelOf(::HowItWorksViewModel)
}
