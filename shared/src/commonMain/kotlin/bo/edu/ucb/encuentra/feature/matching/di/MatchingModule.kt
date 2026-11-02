package bo.edu.ucb.encuentra.feature.matching.di

import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.feature.matching.data.MatchRepositoryImpl
import bo.edu.ucb.encuentra.feature.matching.data.RemoteConfigMatchingConfigRepository
import bo.edu.ucb.encuentra.feature.matching.data.RoomMatchingStore
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchRepository
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingConfigRepository
import bo.edu.ucb.encuentra.feature.matching.domain.repository.MatchingStore
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ClaimMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMatchDetailUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.ObserveMyMatchesUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RejectMatchUseCase
import bo.edu.ucb.encuentra.feature.matching.domain.usecase.RunMatchingUseCase
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchDetailViewModel
import bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel.MatchListViewModel
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante E. Aquí van los repositorios, use cases y ViewModels de esta feature.
val matchingModule = module {
    // Matching (M01-M02): lo llaman el envío del reporte (L04), el registro de hallazgos (H03) y "No es mío".
    single<MatchingConfigRepository> { RemoteConfigMatchingConfigRepository() }
    single<MatchingStore> { RoomMatchingStore(get(), get(), get(), get()) }
    single<MatchingTrigger> { RunMatchingUseCase(get(), get(), get()) }
    singleOf(::RejectMatchUseCase)
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get(), get()) }
    singleOf(::ObserveMyMatchesUseCase)
    singleOf(::ObserveMatchDetailUseCase)
    singleOf(::ClaimMatchUseCase)
    viewModelOf(::MatchListViewModel)
    // El id llega desde la ruta (o el deep link): koinViewModel { parametersOf(matchId) }.
    viewModel { (matchId: String) -> MatchDetailViewModel(ItemId(matchId), get(), get(), get()) }
}
