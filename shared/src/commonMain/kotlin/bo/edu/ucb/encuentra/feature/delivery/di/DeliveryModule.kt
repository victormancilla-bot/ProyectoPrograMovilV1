package bo.edu.ucb.encuentra.feature.delivery.di

import bo.edu.ucb.encuentra.feature.delivery.presentation.viewmodel.DeliveryViewModel
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ConfirmDeliveryUseCase
import bo.edu.ucb.encuentra.feature.delivery.domain.usecase.ScheduleDeliveryUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante B. Aquí van los repositorios, use cases y ViewModels de esta feature.
val deliveryModule = module {
    // V04: programar la entrega y confirmarla con la verificación física.
    singleOf(::ScheduleDeliveryUseCase)
    singleOf(::ConfirmDeliveryUseCase)
    viewModelOf(::DeliveryViewModel)
}
