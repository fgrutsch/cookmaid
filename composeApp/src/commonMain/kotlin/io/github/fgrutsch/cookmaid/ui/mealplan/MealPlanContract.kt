package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

data class MealPlanState(
    val currentWeekStart: LocalDate,
    val days: List<MealPlanDay> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface MealPlanEvent {
    data object Load : MealPlanEvent
    data object PreviousWeek : MealPlanEvent
    data object NextWeek : MealPlanEvent
    data object GoToCurrentWeek : MealPlanEvent
    data class AddRecipeItem(val dayDate: LocalDate, val recipeId: Uuid) : MealPlanEvent
    data class AddNoteItem(val dayDate: LocalDate, val note: String) : MealPlanEvent
    data class UpdateNote(val itemId: Uuid, val dayDate: LocalDate, val newNote: String) : MealPlanEvent
    data class DeleteItem(val itemId: Uuid, val dayDate: LocalDate) : MealPlanEvent
    data class AddIngredientsToShoppingList(val ingredients: List<RecipeIngredient>) : MealPlanEvent
}

sealed interface MealPlanEffect {
    data object IngredientsAdded : MealPlanEffect
    data class Error(val message: String) : MealPlanEffect
}
