package io.github.fgrutsch.cookmaid.ui.recipe.list

import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

data class RecipeListState(
    val initialized: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val searchActive: Boolean = false,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val randomRecipe: Recipe? = null,
)

sealed interface RecipeListEvent {
    data object LoadRecipes : RecipeListEvent
    data object Refresh : RecipeListEvent
    data object LoadMore : RecipeListEvent
    data class UpdateSearchQuery(val query: String) : RecipeListEvent
    data class SetSearchActive(val active: Boolean) : RecipeListEvent
    data class SelectTag(val tag: String?) : RecipeListEvent
    data object RollRandomRecipe : RecipeListEvent
    data object ClearRandomRecipe : RecipeListEvent
    data class DeleteRecipe(val id: Uuid) : RecipeListEvent
    data class AddIngredientsToShoppingList(val ingredients: List<RecipeIngredient>) : RecipeListEvent
    data class AddToMealPlan(val recipeId: Uuid, val day: LocalDate) : RecipeListEvent
}

sealed interface RecipeListEffect {
    data object AddedToShoppingList : RecipeListEffect
    data object AddedToMealPlan : RecipeListEffect
    data class Error(val message: String) : RecipeListEffect
}
