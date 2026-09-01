package io.github.fgrutsch.cookmaid.ui.recipe.detail

import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * @property servings the serving count the ingredients are currently shown for.
 *   Starts at the recipe's configured servings and is not persisted.
 */
data class RecipeDetailState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
    val servings: Int? = null,
)

sealed interface RecipeDetailEvent {
    data object Load : RecipeDetailEvent
    data object Delete : RecipeDetailEvent
    data class AddIngredientsToShoppingList(val ingredients: List<RecipeIngredient>) : RecipeDetailEvent
    data class AddToMealPlan(val recipeId: Uuid, val day: LocalDate) : RecipeDetailEvent
    data class SetServings(val servings: Int) : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object Deleted : RecipeDetailEffect
    data object AddedToShoppingList : RecipeDetailEffect
    data object AddedToMealPlan : RecipeDetailEffect
    data object Error : RecipeDetailEffect
}
