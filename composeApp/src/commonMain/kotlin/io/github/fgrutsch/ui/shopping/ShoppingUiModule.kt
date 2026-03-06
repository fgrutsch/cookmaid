package io.github.fgrutsch.ui.shopping

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val shoppingUiModule = module {
    factoryOf(::ShoppingListViewModel)
}
