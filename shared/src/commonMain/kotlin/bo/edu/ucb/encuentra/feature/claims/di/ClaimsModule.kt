package bo.edu.ucb.encuentra.feature.claims.di

import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimDetailViewModel
import bo.edu.ucb.encuentra.feature.claims.presentation.viewmodel.ClaimsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante A. Aquí van los repositorios, use cases y ViewModels de esta feature.
val claimsModule = module {
    viewModelOf(::ClaimsViewModel)
    viewModelOf(::ClaimDetailViewModel)
}
