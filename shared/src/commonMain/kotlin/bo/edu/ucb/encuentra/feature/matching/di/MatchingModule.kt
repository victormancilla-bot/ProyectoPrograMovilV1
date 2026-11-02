package bo.edu.ucb.encuentra.feature.matching.di

import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.feature.matching.data.PendingMatchingTrigger
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante E. Aquí van los repositorios, use cases y ViewModels de esta feature.
val matchingModule = module {
    // Hook que dispara el envío del reporte (L04); M01 cambia esta implementación por el matching real.
    single<MatchingTrigger> { PendingMatchingTrigger() }
    viewModelOf(::MatchListViewModel)
    viewModelOf(::MatchDetailViewModel)
}
