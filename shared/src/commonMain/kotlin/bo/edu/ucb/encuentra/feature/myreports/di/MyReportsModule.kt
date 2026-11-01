package bo.edu.ucb.encuentra.feature.myreports.di

import bo.edu.ucb.encuentra.feature.myreports.presentation.viewmodel.MyReportsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val myReportsModule = module {
    viewModelOf(::MyReportsViewModel)
}
