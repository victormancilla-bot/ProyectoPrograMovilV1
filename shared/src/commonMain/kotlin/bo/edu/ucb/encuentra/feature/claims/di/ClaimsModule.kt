package bo.edu.ucb.encuentra.feature.claims.di

import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.feature.claims.data.ClaimRepositoryImpl
import bo.edu.ucb.encuentra.feature.claims.domain.repository.ClaimRepository
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ApproveClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.ChangeClaimStatusUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.CreateClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.DeliverClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.RejectClaimUseCase
import bo.edu.ucb.encuentra.feature.claims.domain.usecase.SubmitClaimAnswersUseCase
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val claimsModule = module {
    // Reclamos (V02): crear desde "Esto es mío — verificar" (M05) y transiciones PENDIENTE → ... → ENTREGADO.
    single<ClaimRepository> { ClaimRepositoryImpl(get(), get(), get()) }
    singleOf(::CreateClaimUseCase)
    single<ClaimRequester> { get<CreateClaimUseCase>() }
    singleOf(::ChangeClaimStatusUseCase)
    singleOf(::SubmitClaimAnswersUseCase)
    singleOf(::ApproveClaimUseCase)
    singleOf(::RejectClaimUseCase)
    singleOf(::DeliverClaimUseCase)
    viewModelOf(::ClaimsViewModel)
    viewModelOf(::ClaimDetailViewModel)
}
