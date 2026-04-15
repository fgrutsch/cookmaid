package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val recipeModule = module {
    singleOf(::ApiRecipeClient) bind RecipeClient::class
    singleOf(::ApiRecipeRepository) bind RecipeRepository::class
    singleOf(::RecipeListViewModel)
}
