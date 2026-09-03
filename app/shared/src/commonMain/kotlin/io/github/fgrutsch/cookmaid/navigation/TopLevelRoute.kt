package io.github.fgrutsch.cookmaid.navigation

import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.ic_account_circle
import cookmaid.app.shared.generated.resources.ic_calendar_month
import cookmaid.app.shared.generated.resources.ic_menu_book
import cookmaid.app.shared.generated.resources.ic_shopping_cart
import cookmaid.app.shared.generated.resources.nav_account
import cookmaid.app.shared.generated.resources.nav_meal_plan
import cookmaid.app.shared.generated.resources.nav_recipes
import cookmaid.app.shared.generated.resources.nav_shopping
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * The bottom-navigation destinations.
 *
 * The settings screen is one of them, labelled Account: it holds the profile, sign-out and
 * account deletion alongside theme and locale, so it is a place rather than a screen action.
 * Anything in a top app bar inherits that bar's screen scope, which made a settings entry there
 * read as settings *for the current tab*. The nav bar is where places belong.
 */
enum class TopLevelRoute(
    val labelRes: StringResource,
    val icon: DrawableResource,
    val startRoute: Route,
) {
    Shopping(Res.string.nav_shopping, Res.drawable.ic_shopping_cart, Route.ShoppingList),
    Recipes(Res.string.nav_recipes, Res.drawable.ic_menu_book, Route.RecipeList),
    MealPlan(Res.string.nav_meal_plan, Res.drawable.ic_calendar_month, Route.MealPlan),
    Account(Res.string.nav_account, Res.drawable.ic_account_circle, Route.Settings),
}
