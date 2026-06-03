package io.github.fgrutsch.cookmaid.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

sealed interface Route : NavKey {
    @Serializable
    data object ShoppingList : Route

    @Serializable
    data object RecipeList : Route

    @Serializable
    data object MealPlan : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class RecipeDetail(val id: Uuid) : Route

    @Serializable
    data object AddRecipe : Route

    @Serializable
    data class EditRecipe(val id: Uuid) : Route
}
