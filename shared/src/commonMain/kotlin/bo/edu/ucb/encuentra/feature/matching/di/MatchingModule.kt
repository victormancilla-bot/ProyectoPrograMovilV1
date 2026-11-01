package bo.edu.ucb.encuentra.feature.matching.di

import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante E. Aquí van los repositorios, use cases y ViewModels de esta feature.
val matchingModule = module {
    viewModelOf(::MatchListViewModel)
    viewModelOf(::MatchDetailViewModel)
}
