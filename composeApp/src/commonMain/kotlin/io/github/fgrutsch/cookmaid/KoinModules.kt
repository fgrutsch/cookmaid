package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.di.sharedModules
import io.github.fgrutsch.cookmaid.ui.auth.authModule
import io.github.fgrutsch.cookmaid.ui.catalog.catalogModule
import io.github.fgrutsch.cookmaid.ui.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.ui.recipe.recipeModule
import io.github.fgrutsch.cookmaid.ui.settings.settingsModule
import io.github.fgrutsch.cookmaid.ui.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.ui.user.userModule

val allModules = sharedModules + listOf(authModule, userModule, catalogModule, settingsModule, shoppingModule, recipeModule, mealPlanModule)
