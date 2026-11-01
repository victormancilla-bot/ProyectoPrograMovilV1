package bo.edu.ucb.encuentra.core.di

import bo.edu.ucb.encuentra.core.data.SessionRepositoryImpl
import bo.edu.ucb.encuentra.core.domain.repository.SessionRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

// Lo que comparten todas las features: sesión del usuario.
val coreModule = module {
    single<SessionRepository> { SessionRepositoryImpl() }
    singleOf(::GetCurrentUserUseCase)
}
