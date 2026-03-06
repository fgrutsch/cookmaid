package io.github.fgrutsch.di

import io.github.fgrutsch.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.ui.recipe.AddRecipeViewModel
import io.github.fgrutsch.ui.recipe.RecipeDetailViewModel
import io.github.fgrutsch.ui.recipe.RecipeListViewModel
import io.github.fgrutsch.ui.settings.SettingsViewModel
import io.github.fgrutsch.ui.shopping.ShoppingListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::SettingsViewModel)
    factoryOf(::ShoppingListViewModel)
    factoryOf(::RecipeListViewModel)
    factory { (recipeId: String) -> RecipeDetailViewModel(recipeId, get(), get(), get()) }
    factory { AddRecipeViewModel(get(), get(), get()) }
    factoryOf(::MealPlanViewModel)
}
