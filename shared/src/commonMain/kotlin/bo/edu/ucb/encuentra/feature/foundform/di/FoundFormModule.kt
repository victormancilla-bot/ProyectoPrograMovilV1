package bo.edu.ucb.encuentra.feature.foundform.di

import bo.edu.ucb.encuentra.feature.foundform.data.FoundItemRepositoryImpl
import bo.edu.ucb.encuentra.feature.foundform.domain.repository.FoundItemRepository
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.RegisterFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Formulario "Registrar Objeto" (H03): validación, guardado en Room y ViewModel.
val foundFormModule = module {
    singleOf(::FoundItemRepositoryImpl) { bind<FoundItemRepository>() }
    singleOf(::ValidateFoundItemUseCase)
    singleOf(::RegisterFoundItemUseCase)
    viewModelOf(::FoundFormViewModel)
}
