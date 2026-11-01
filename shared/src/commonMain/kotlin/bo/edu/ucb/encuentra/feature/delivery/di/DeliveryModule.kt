package bo.edu.ucb.encuentra.feature.delivery.di

import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val deliveryModule = module {
    viewModelOf(::DeliveryViewModel)
}
