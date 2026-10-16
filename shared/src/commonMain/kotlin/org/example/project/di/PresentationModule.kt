package org.example.project.di

import org.example.project.features.login.presentation.viewmodel.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::LoginViewModel)
}
