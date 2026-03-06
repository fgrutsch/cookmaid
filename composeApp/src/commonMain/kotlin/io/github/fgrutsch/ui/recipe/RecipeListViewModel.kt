package io.github.fgrutsch.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.mealplan.MealPlanItem
import io.github.fgrutsch.mealplan.MealPlanRepository
import io.github.fgrutsch.mealplan.mondayOfWeek
import io.github.fgrutsch.recipe.Recipe
import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.recipe.RecipeRepository
import io.github.fgrutsch.recipe.TagRepository
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository,
    private val tagRepository: TagRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val mealPlanRepository: MealPlanRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    private val _randomRecipe = MutableStateFlow<Recipe?>(null)
    val randomRecipe: StateFlow<Recipe?> = _randomRecipe.asStateFlow()

    val tags: StateFlow<List<String>> = tagRepository.tags

    val filteredRecipes: StateFlow<List<Recipe>> = combine(
        recipeRepository.recipes,
        _searchQuery,
        _selectedTags,
    ) { recipes, query, selectedTags ->
        var result = recipes
        if (selectedTags.isNotEmpty()) {
            result = result.filter { recipe ->
                recipe.tags.any { it in selectedTags }
            }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _searchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun toggleTagFilter(tag: String) {
        _selectedTags.value = if (tag in _selectedTags.value) {
            _selectedTags.value - tag
        } else {
            _selectedTags.value + tag
        }
    }

    fun rollRandomRecipe() {
        val recipes = recipeRepository.recipes.value
        if (recipes.isEmpty()) return
        val current = _randomRecipe.value
        val candidates = if (recipes.size > 1) recipes.filter { it.id != current?.id } else recipes
        _randomRecipe.value = candidates.random()
    }

    fun clearRandomRecipe() {
        _randomRecipe.value = null
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            recipeRepository.delete(id)
        }
    }

    fun resolveRecipeIngredients(recipeId: String): List<RecipeIngredient> {
        return recipeRepository.recipes.value.find { it.id == recipeId }?.ingredients ?: emptyList()
    }

    fun resolveRecipeName(recipeId: String): String {
        return recipeRepository.recipes.value.find { it.id == recipeId }?.name ?: "Unknown recipe"
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
        return day.items.map { item ->
            when (item) {
                is MealPlanItem.RecipeItem -> resolveRecipeName(item.recipeId)
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
