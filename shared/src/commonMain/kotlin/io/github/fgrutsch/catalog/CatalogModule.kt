package io.github.fgrutsch.catalog

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val catalogModule = module {
    singleOf(::InMemoryCatalogRepository) bind CatalogRepository::class
    singleOf(::InMemoryItemCategoryRepository) bind ItemCategoryRepository::class
}
