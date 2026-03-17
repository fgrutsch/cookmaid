package io.github.fgrutsch.cookmaid.ui.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.MealPlanRepository
import io.github.fgrutsch.cookmaid.mealplan.MealPlanWeek
import io.github.fgrutsch.cookmaid.mealplan.createEmptyWeek
import io.github.fgrutsch.cookmaid.mealplan.mondayOfWeek
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListRepository
import io.github.fgrutsch.cookmaid.ui.common.addIngredientsToDefaultShoppingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MealPlanViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val currentWeekStart: StateFlow<LocalDate>
        field = MutableStateFlow(mondayOfWeek(today))

    val currentWeek: StateFlow<MealPlanWeek> = combine(
        mealPlanRepository.weeks,
        currentWeekStart,
    ) { weeks, weekStart ->
        weeks.find { it.startDate == weekStart } ?: createEmptyWeek(weekStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), createEmptyWeek(currentWeekStart.value))

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes

    init {
        viewModelScope.launch {
            _recipes.value = recipeRepository.fetchPage(cursor = null, limit = 100, search = null, tag = null).items
        }
    }

    fun previousWeek() {
        currentWeekStart.value = currentWeekStart.value.plus(-7, DateTimeUnit.DAY)
    }

    fun nextWeek() {
        currentWeekStart.value = currentWeekStart.value.plus(7, DateTimeUnit.DAY)
    }

    fun goToCurrentWeek() {
        currentWeekStart.value = mondayOfWeek(today)
    }

    fun resolveRecipeName(recipeId: Uuid): String {
        return _recipes.value.find { it.id == recipeId }?.name ?: "Unknown recipe"
    }

    fun resolveRecipeIngredients(recipeId: Uuid): List<RecipeIngredient> {
        return _recipes.value.find { it.id == recipeId }?.ingredients ?: emptyList()
    }

    fun addRecipeItem(dayDate: LocalDate, recipeId: Uuid) {
        viewModelScope.launch {
            mealPlanRepository.getOrCreateWeek(currentWeekStart.value)
            mealPlanRepository.addItem(
                currentWeekStart.value,
                dayDate,
                MealPlanItem.RecipeItem(id = Uuid.random(), recipeId = recipeId),
            )
        }
    }

    fun addNoteItem(dayDate: LocalDate, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mealPlanRepository.getOrCreateWeek(currentWeekStart.value)
            mealPlanRepository.addItem(
                currentWeekStart.value,
                dayDate,
                MealPlanItem.NoteItem(id = Uuid.random(), name = name.trim()),
            )
        }
    }

    fun updateNoteItem(dayDate: LocalDate, itemId: Uuid, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            mealPlanRepository.updateItem(
                currentWeekStart.value,
                dayDate,
                itemId,
                MealPlanItem.NoteItem(id = itemId, name = newName.trim()),
            )
        }
    }

    fun removeItem(dayDate: LocalDate, itemId: Uuid) {
        viewModelScope.launch {
            mealPlanRepository.removeItem(currentWeekStart.value, dayDate, itemId)
        }
    }

    fun addIngredientsToShoppingList(ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
        }
    }
}
