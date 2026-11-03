package bo.edu.ucb.encuentra.feature.audit.di

import bo.edu.ucb.encuentra.feature.audit.data.AuditTrailRepositoryImpl
import bo.edu.ucb.encuentra.feature.audit.domain.repository.AuditTrailRepository
import bo.edu.ucb.encuentra.feature.audit.domain.usecase.ObserveAuditTrailUseCase
import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val auditModule = module {
    // S05: la escritura es AuditRecorder (coreModule); aquí solo la lectura para el visor.
    single<AuditTrailRepository> { AuditTrailRepositoryImpl(get(), get(), get(), get(), get()) }
    singleOf(::ObserveAuditTrailUseCase)
    viewModelOf(::AuditViewModel)
}
