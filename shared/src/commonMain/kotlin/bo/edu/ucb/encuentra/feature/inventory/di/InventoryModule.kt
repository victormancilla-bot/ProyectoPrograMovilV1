package bo.edu.ucb.encuentra.feature.inventory.di

import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import bo.edu.ucb.encuentra.feature.inventory.data.InventoryRepositoryImpl
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.InventoryRepository
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryItemUseCase
import bo.edu.ucb.encuentra.feature.inventory.domain.usecase.ObserveInventoryUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante D. Aquí van los repositorios, use cases y ViewModels de esta feature.
val inventoryModule = module {
    // H07: inventario en vivo (Room); editar la custodia usa MoveCustodyUseCase de H05 (coreModule).
    single<InventoryRepository> { InventoryRepositoryImpl(get(), get(), get()) }
    singleOf(::ObserveInventoryUseCase)
    singleOf(::ObserveInventoryItemUseCase)
    viewModelOf(::InventoryViewModel)
}
