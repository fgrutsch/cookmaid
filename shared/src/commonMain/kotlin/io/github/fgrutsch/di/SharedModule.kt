package io.github.fgrutsch.di

import io.github.fgrutsch.catalog.catalogModule
import io.github.fgrutsch.mealplan.mealPlanModule
import io.github.fgrutsch.recipe.recipeModule
import io.github.fgrutsch.shopping.shoppingModule

val sharedModules = listOf(catalogModule, shoppingModule, recipeModule, mealPlanModule)
