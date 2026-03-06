package io.github.fgrutsch.di

import io.github.fgrutsch.mealplan.InMemoryMealPlanRepository
import io.github.fgrutsch.mealplan.MealPlanRepository
import io.github.fgrutsch.catalog.CatalogRepository
import io.github.fgrutsch.catalog.InMemoryCatalogRepository
import io.github.fgrutsch.catalog.InMemoryItemCategoryRepository
import io.github.fgrutsch.catalog.ItemCategoryRepository
import io.github.fgrutsch.recipe.InMemoryRecipeRepository
import io.github.fgrutsch.recipe.InMemoryTagRepository
import io.github.fgrutsch.recipe.RecipeRepository
import io.github.fgrutsch.recipe.TagRepository
import io.github.fgrutsch.shopping.InMemoryShoppingListRepository
import io.github.fgrutsch.shopping.ShoppingListRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    singleOf(::InMemoryCatalogRepository) bind CatalogRepository::class
    singleOf(::InMemoryItemCategoryRepository) bind ItemCategoryRepository::class
    singleOf(::InMemoryShoppingListRepository) bind ShoppingListRepository::class
    singleOf(::InMemoryRecipeRepository) bind RecipeRepository::class
    singleOf(::InMemoryTagRepository) bind TagRepository::class
    singleOf(::InMemoryMealPlanRepository) bind MealPlanRepository::class
}
