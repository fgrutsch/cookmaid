package io.github.fgrutsch.ui.shopping

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val shoppingModule = module {
    singleOf(::ShoppingListClient)
    singleOf(::ApiShoppingListRepository) bind ShoppingListRepository::class
    factoryOf(::ShoppingListViewModel)
}
