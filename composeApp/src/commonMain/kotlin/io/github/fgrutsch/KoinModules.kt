package io.github.fgrutsch

import io.github.fgrutsch.di.sharedModules
import io.github.fgrutsch.ui.auth.authModule
import io.github.fgrutsch.ui.catalog.catalogModule
import io.github.fgrutsch.ui.mealplan.mealPlanModule
import io.github.fgrutsch.ui.recipe.recipeModule
import io.github.fgrutsch.ui.settings.settingsModule
import io.github.fgrutsch.ui.shopping.shoppingModule
import io.github.fgrutsch.ui.user.userModule

val allModules = sharedModules + listOf(authModule, userModule, catalogModule, settingsModule, shoppingModule, recipeModule, mealPlanModule)
