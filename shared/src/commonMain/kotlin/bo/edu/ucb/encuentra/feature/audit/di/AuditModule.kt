package bo.edu.ucb.encuentra.feature.audit.di

import bo.edu.ucb.encuentra.feature.audit.presentation.viewmodel.AuditViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val auditModule = module {
    viewModelOf(::AuditViewModel)
}
