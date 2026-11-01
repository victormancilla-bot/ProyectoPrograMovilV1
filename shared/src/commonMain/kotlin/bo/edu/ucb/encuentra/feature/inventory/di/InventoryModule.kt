package bo.edu.ucb.encuentra.feature.inventory.di

import bo.edu.ucb.encuentra.feature.inventory.presentation.viewmodel.InventoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Dueño: Integrante D. Aquí van los repositorios, use cases y ViewModels de esta feature.
val inventoryModule = module {
    viewModelOf(::InventoryViewModel)
}
