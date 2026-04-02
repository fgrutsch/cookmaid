package io.github.fgrutsch.cookmaid.navigation

import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.ic_calendar_month
import cookmaid.composeapp.generated.resources.ic_menu_book
import cookmaid.composeapp.generated.resources.ic_settings
import cookmaid.composeapp.generated.resources.ic_shopping_cart
import cookmaid.composeapp.generated.resources.nav_meal_plan
import cookmaid.composeapp.generated.resources.nav_recipes
import cookmaid.composeapp.generated.resources.nav_settings
import cookmaid.composeapp.generated.resources.nav_shopping
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class TopLevelRoute(
    val labelRes: StringResource,
    val icon: DrawableResource,
    val startRoute: Route,
) {
    Shopping(Res.string.nav_shopping, Res.drawable.ic_shopping_cart, Route.ShoppingList),
    Recipes(Res.string.nav_recipes, Res.drawable.ic_menu_book, Route.RecipeList),
    MealPlan(Res.string.nav_meal_plan, Res.drawable.ic_calendar_month, Route.MealPlan),
    Settings(Res.string.nav_settings, Res.drawable.ic_settings, Route.Settings),
}
