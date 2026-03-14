package io.github.fgrutsch.ui.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.mealplan.MealPlanItem
import io.github.fgrutsch.mealplan.MealPlanRepository
import io.github.fgrutsch.mealplan.MealPlanWeek
import io.github.fgrutsch.mealplan.createEmptyWeek
import io.github.fgrutsch.mealplan.mondayOfWeek
import io.github.fgrutsch.recipe.Recipe
import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.recipe.RecipeRepository
import io.github.fgrutsch.ui.shopping.ShoppingListRepository
import io.github.fgrutsch.ui.common.addIngredientsToDefaultShoppingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MealPlanViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val _currentWeekStart = MutableStateFlow(mondayOfWeek(today))
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart.asStateFlow()

    val currentWeek: StateFlow<MealPlanWeek> = combine(
        mealPlanRepository.weeks,
        _currentWeekStart,
    ) { weeks, weekStart ->
        weeks.find { it.startDate == weekStart } ?: createEmptyWeek(weekStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), createEmptyWeek(_currentWeekStart.value))

    val recipes: StateFlow<List<Recipe>> = recipeRepository.recipes

    fun previousWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plus(-7, DateTimeUnit.DAY)
    }

    fun nextWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plus(7, DateTimeUnit.DAY)
    }

    fun goToCurrentWeek() {
        _currentWeekStart.value = mondayOfWeek(today)
    }

    fun resolveRecipeName(recipeId: String): String {
        return recipeRepository.recipes.value.find { it.id == recipeId }?.name ?: "Unknown recipe"
    }

    fun resolveRecipeIngredients(recipeId: String): List<RecipeIngredient> {
        return recipeRepository.recipes.value.find { it.id == recipeId }?.ingredients ?: emptyList()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addRecipeItem(dayDate: LocalDate, recipeId: String) {
        viewModelScope.launch {
            mealPlanRepository.getOrCreateWeek(_currentWeekStart.value)
            mealPlanRepository.addItem(
                _currentWeekStart.value,
                dayDate,
                MealPlanItem.RecipeItem(id = Uuid.random().toString(), recipeId = recipeId),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addNoteItem(dayDate: LocalDate, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mealPlanRepository.getOrCreateWeek(_currentWeekStart.value)
            mealPlanRepository.addItem(
                _currentWeekStart.value,
                dayDate,
                MealPlanItem.NoteItem(id = Uuid.random().toString(), name = name.trim()),
            )
        }
    }

    fun updateNoteItem(dayDate: LocalDate, itemId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            mealPlanRepository.updateItem(
                _currentWeekStart.value,
                dayDate,
                itemId,
                MealPlanItem.NoteItem(id = itemId, name = newName.trim()),
            )
        }
    }

    fun removeItem(dayDate: LocalDate, itemId: String) {
        viewModelScope.launch {
            mealPlanRepository.removeItem(_currentWeekStart.value, dayDate, itemId)
        }
    }

    fun addIngredientsToShoppingList(ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
        }
    }
}
