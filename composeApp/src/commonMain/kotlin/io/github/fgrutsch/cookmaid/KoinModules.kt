package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.ui.auth.authModule
import io.github.fgrutsch.cookmaid.ui.catalog.catalogModule
import io.github.fgrutsch.cookmaid.ui.mealplan.mealPlanModule
import io.github.fgrutsch.cookmaid.ui.recipe.recipeModule
import io.github.fgrutsch.cookmaid.ui.settings.settingsModule
import io.github.fgrutsch.cookmaid.ui.shopping.shoppingModule
import io.github.fgrutsch.cookmaid.ui.user.userModule

/**
 * Modules whose beans outlive a login session: auth infrastructure,
 * user-account client, device-wide settings. Loaded once at app start.
 */
val appModules = listOf(
    authModule,
    userModule,
    settingsModule,
)

/**
 * Modules whose beans hold user-scoped state (caches, paginated lists,
 * singleton ViewModels). Loaded inside a Koin scope keyed by `user.id`
 * in `App.kt` so the graph is rebuilt on login and garbage-collected on
 * logout — no manual `clear()`/`resetState()` needed.
 *
 * New user-scoped repository or singleton ViewModel? Add it to a feature
 * module here. Anything that must survive logout (auth, settings, platform
 * bindings) belongs in [appModules] instead.
 */
val sessionModules = listOf(
    catalogModule,
    shoppingModule,
    recipeModule,
    mealPlanModule,
)
