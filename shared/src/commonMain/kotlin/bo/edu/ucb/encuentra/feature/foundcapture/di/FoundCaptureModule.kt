package bo.edu.ucb.encuentra.feature.foundcapture.di

import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante D. Aquí van los repositorios, use cases y ViewModels de esta feature.
val foundCaptureModule = module {
    viewModelOf(::FoundCaptureViewModel)
}
