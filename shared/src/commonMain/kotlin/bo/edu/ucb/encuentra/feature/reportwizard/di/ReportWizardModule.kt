package bo.edu.ucb.encuentra.feature.reportwizard.di

import bo.edu.ucb.encuentra.feature.reportwizard.data.LostReportRepositoryImpl
import bo.edu.ucb.encuentra.feature.reportwizard.domain.repository.LostReportRepository
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.SubmitLostReportUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase.ValidateReportStepUseCase
import bo.edu.ucb.encuentra.feature.reportwizard.presentation.viewmodel.ReportWizardViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante C. Aquí van los repositorios, use cases y ViewModels de esta feature.
val reportWizardModule = module {
    singleOf(::ValidateReportStepUseCase)
    single<LostReportRepository> { LostReportRepositoryImpl(get()) }
    singleOf(::SubmitLostReportUseCase)
    viewModelOf(::ReportWizardViewModel)
}
