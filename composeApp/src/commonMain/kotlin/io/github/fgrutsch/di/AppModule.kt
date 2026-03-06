package io.github.fgrutsch.di

import io.github.fgrutsch.ui.mealplan.mealPlanUiModule
import io.github.fgrutsch.ui.recipe.recipeUiModule
import io.github.fgrutsch.ui.settings.settingsUiModule
import io.github.fgrutsch.ui.shopping.shoppingUiModule

val appModules = listOf(settingsUiModule, shoppingUiModule, recipeUiModule, mealPlanUiModule)
