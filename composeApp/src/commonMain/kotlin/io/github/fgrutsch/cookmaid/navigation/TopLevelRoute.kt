package io.github.fgrutsch.cookmaid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.nav_meal_plan
import cookmaid.composeapp.generated.resources.nav_recipes
import cookmaid.composeapp.generated.resources.nav_settings
import cookmaid.composeapp.generated.resources.nav_shopping
import org.jetbrains.compose.resources.StringResource

enum class TopLevelRoute(
    val labelRes: StringResource,
    val icon: ImageVector,
    val startRoute: Route,
) {
    Shopping(Res.string.nav_shopping, Icons.Default.ShoppingCart, Route.ShoppingList),
    Recipes(Res.string.nav_recipes, Icons.AutoMirrored.Filled.MenuBook, Route.RecipeList),
    MealPlan(Res.string.nav_meal_plan, Icons.Default.CalendarMonth, Route.MealPlan),
    Settings(Res.string.nav_settings, Icons.Default.Settings, Route.Settings),
}
