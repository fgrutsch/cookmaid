package io.github.fgrutsch.cookmaid.ui.recipe.edit

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient

data class AddRecipeState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val nameError: Boolean = false,
    val description: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val servings: Int? = null,
    val ingredientQuery: String = "",
    val ingredientSuggestions: List<Item.Catalog> = emptyList(),
)

sealed interface AddRecipeEvent {
    data object Load : AddRecipeEvent
    data class SetName(val value: String) : AddRecipeEvent
    data class SetDescription(val value: String) : AddRecipeEvent
    data class UpdateIngredientQuery(val query: String) : AddRecipeEvent
    data class AddIngredient(val item: Item, val quantity: String?) : AddRecipeEvent
    data class UpdateIngredientQuantity(val index: Int, val quantity: String?) : AddRecipeEvent
    data class SetServings(val value: Int?) : AddRecipeEvent
    data class RemoveIngredient(val index: Int) : AddRecipeEvent
    data class AddStep(val step: String) : AddRecipeEvent
    data class RemoveStep(val index: Int) : AddRecipeEvent
    data class ToggleTag(val tag: String) : AddRecipeEvent
    data class CreateAndAddTag(val tag: String) : AddRecipeEvent
    data object Save : AddRecipeEvent
}

sealed interface AddRecipeEffect {
    data object Saved : AddRecipeEffect
    data class Error(val message: String) : AddRecipeEffect
}
