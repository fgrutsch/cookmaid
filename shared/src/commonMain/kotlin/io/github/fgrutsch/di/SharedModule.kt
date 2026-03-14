package io.github.fgrutsch.di

import io.github.fgrutsch.mealplan.mealPlanModule
import io.github.fgrutsch.recipe.recipeModule

val sharedModules = listOf(recipeModule, mealPlanModule)
