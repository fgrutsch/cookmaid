package io.github.fgrutsch.cookmaid.ui.auth

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Dedicated module for the global [SessionCleaner]. Lives outside
 * [authModule] so the auth package does not gain compile-time imports to
 * the feature packages ([SessionCleaner] depends on repositories and
 * ViewModels from shopping, catalog, mealplan, and recipe).
 */
val sessionModule = module {
    singleOf(::SessionCleaner)
}
