package io.github.fgrutsch.cookmaid.di

import io.github.fgrutsch.cookmaid.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.recipe.recipeModule

val sharedModules = listOf(recipeModule, mealPlanModule)
