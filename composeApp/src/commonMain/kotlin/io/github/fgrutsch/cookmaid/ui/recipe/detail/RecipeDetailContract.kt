package io.github.fgrutsch.cookmaid.ui.recipe.detail

import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient

data class RecipeDetailState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
)

sealed interface RecipeDetailEvent {
    data object Load : RecipeDetailEvent
    data object Delete : RecipeDetailEvent
    data class AddIngredientsToShoppingList(val ingredients: List<RecipeIngredient>) : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object Deleted : RecipeDetailEffect
    data object AddedToShoppingList : RecipeDetailEffect
    data class Error(val message: String) : RecipeDetailEffect
}
