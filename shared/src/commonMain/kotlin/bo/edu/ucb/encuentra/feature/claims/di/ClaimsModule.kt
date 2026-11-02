package bo.edu.ucb.encuentra.feature.claims.di

import bo.edu.ucb.encuentra.core.domain.repository.ClaimRequester
import bo.edu.ucb.encuentra.feature.claims.data.PendingClaimRequester
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val claimsModule = module {
    // "Esto es mío — verificar" (M05). V02 reemplaza el placeholder por la creación real del reclamo.
    single<ClaimRequester> { PendingClaimRequester() }
    viewModelOf(::ClaimsViewModel)
    viewModelOf(::ClaimDetailViewModel)
}
