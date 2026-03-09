package io.github.fgrutsch.shopping

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val shoppingModule = module {
    singleOf(::InMemoryShoppingListRepository) bind ShoppingListRepository::class
}
