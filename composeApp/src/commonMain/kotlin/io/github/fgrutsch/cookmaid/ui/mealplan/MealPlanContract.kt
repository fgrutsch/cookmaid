package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

data class MealPlanState(
    val currentWeekStart: LocalDate,
    val days: List<MealPlanDay> = emptyList(),
    val initialized: Boolean = false,
    val isLoading: Boolean = false,
    val recipeSearchResults: List<Recipe> = emptyList(),
)

sealed interface MealPlanEvent {
    data object Load : MealPlanEvent
    data object Refresh : MealPlanEvent
    data object PreviousWeek : MealPlanEvent
    data object NextWeek : MealPlanEvent
    data object GoToCurrentWeek : MealPlanEvent
    data class SearchRecipes(val query: String) : MealPlanEvent
    data class AddRecipeItem(val day: LocalDate, val recipeId: Uuid) : MealPlanEvent
    data class AddNoteItem(val day: LocalDate, val note: String) : MealPlanEvent
    data class UpdateNote(val itemId: Uuid, val day: LocalDate, val newNote: String) : MealPlanEvent
    data class DeleteItem(val itemId: Uuid, val day: LocalDate) : MealPlanEvent
    data class AddRecipeToShoppingList(val recipeId: Uuid, val recipeName: String) : MealPlanEvent
}

sealed interface MealPlanEffect {
    data object IngredientsAdded : MealPlanEffect
    data class ShowIngredientPicker(val recipeName: String, val ingredients: List<RecipeIngredient>) : MealPlanEffect
    data class Error(val message: String) : MealPlanEffect
}
