package io.github.fgrutsch.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.mealplan.MealPlanItem
import io.github.fgrutsch.mealplan.MealPlanRepository
import io.github.fgrutsch.mealplan.mondayOfWeek
import io.github.fgrutsch.recipe.Recipe
import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.recipe.RecipeRepository
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecipeDetailViewModel(
    private val recipeId: String,
    private val recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val mealPlanRepository: MealPlanRepository,
) : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    init {
        viewModelScope.launch {
            _recipe.value = recipeRepository.getById(recipeId)
        }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            _recipe.value?.let { recipeRepository.delete(it.id) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addIngredientsToShoppingList(ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            val lists = shoppingListRepository.lists.value
            val targetListId = lists.find { it.default }?.id ?: lists.firstOrNull()?.id ?: return@launch
            ingredients.forEach { ingredient ->
                shoppingListRepository.addItem(
                    targetListId,
                    ShoppingItem(id = Uuid.random().toString(), item = ingredient.item, quantity = ingredient.quantity),
                )
            }
        }
    }

    fun resolveMealPlanDayItems(date: LocalDate): List<String> {
        val weekStart = mondayOfWeek(date)
        val week = mealPlanRepository.weeks.value.find { it.startDate == weekStart }
        val day = week?.days?.find { it.date == date } ?: return emptyList()
        val recipes = recipeRepository.recipes.value
        return day.items.map { item ->
            when (item) {
                is MealPlanItem.RecipeItem -> recipes.find { it.id == item.recipeId }?.name ?: "Unknown recipe"
                is MealPlanItem.NoteItem -> item.name
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addRecipeToMealPlan(recipeId: String, dayDate: LocalDate) {
        viewModelScope.launch {
            val weekStart = mondayOfWeek(dayDate)
            mealPlanRepository.getOrCreateWeek(weekStart)
            mealPlanRepository.addItem(
                weekStart,
                dayDate,
                MealPlanItem.RecipeItem(id = Uuid.random().toString(), recipeId = recipeId),
            )
        }
    }
}
