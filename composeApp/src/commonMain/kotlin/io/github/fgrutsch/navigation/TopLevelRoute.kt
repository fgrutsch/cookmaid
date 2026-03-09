package io.github.fgrutsch.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelRoute(
    val label: String,
    val icon: ImageVector,
    val startRoute: Route,
) {
    Shopping("Shopping", Icons.Default.ShoppingCart, Route.ShoppingList),
    Recipes("Recipes", Icons.AutoMirrored.Filled.MenuBook, Route.RecipeList),
    MealPlan("Meal Plan", Icons.Default.CalendarMonth, Route.MealPlan),
    Settings("Settings", Icons.Default.Settings, Route.Settings),
}
