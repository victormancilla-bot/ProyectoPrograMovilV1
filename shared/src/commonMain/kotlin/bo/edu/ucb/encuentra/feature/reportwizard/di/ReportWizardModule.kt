package bo.edu.ucb.encuentra.feature.reportwizard.di

import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val reportWizardModule = module {
    viewModelOf(::ReportWizardViewModel)
}
