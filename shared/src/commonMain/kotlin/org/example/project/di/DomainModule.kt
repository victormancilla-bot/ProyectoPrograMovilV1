package org.example.project.di

import org.example.project.features.login.domain.usecase.AuthenticateUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    singleOf(::AuthenticateUseCase)
}
