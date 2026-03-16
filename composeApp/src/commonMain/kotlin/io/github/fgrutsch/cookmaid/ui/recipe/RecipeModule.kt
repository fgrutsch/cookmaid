package io.github.fgrutsch.cookmaid.ui.recipe

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val recipeModule = module {
    factoryOf(::RecipeListViewModel)
    factory { (recipeId: String) -> RecipeDetailViewModel(recipeId, get(), get(), get()) }
    factory { AddRecipeViewModel(get(), get(), get()) }
}
