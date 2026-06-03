package io.github.fgrutsch.cookmaid.ui.catalog

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val catalogModule = module {
    singleOf(::CatalogItemClient)
    singleOf(::ApiCatalogItemRepository) bind CatalogItemRepository::class
}
