package bo.edu.ucb.encuentra.feature.profile.di

import bo.edu.ucb.encuentra.feature.profile.domain.usecase.AssignStaffByEmailUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ChangeUserRoleUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.LogoutUseCase
import bo.edu.ucb.encuentra.feature.profile.domain.usecase.ObserveUsersUseCase
import bo.edu.ucb.encuentra.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante E. Aquí van los repositorios, use cases y ViewModels de esta feature.
val profileModule = module {
    singleOf(::LogoutUseCase)
    // S03: panel de roles del admin.
    singleOf(::ObserveUsersUseCase)
    singleOf(::ChangeUserRoleUseCase)
    singleOf(::AssignStaffByEmailUseCase)
    viewModelOf(::ProfileViewModel)
}
