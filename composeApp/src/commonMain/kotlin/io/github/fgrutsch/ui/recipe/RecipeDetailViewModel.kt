package io.github.fgrutsch.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.mealplan.MealPlanRepository
import io.github.fgrutsch.recipe.Recipe
import io.github.fgrutsch.recipe.RecipeIngredient
import io.github.fgrutsch.recipe.RecipeRepository
import io.github.fgrutsch.shopping.ShoppingListRepository
import io.github.fgrutsch.ui.common.addIngredientsToDefaultShoppingList
import io.github.fgrutsch.ui.common.addRecipeToMealPlan
import io.github.fgrutsch.ui.common.resolveMealPlanDayItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

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

    fun addIngredientsToShoppingList(ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
        }
    }

    fun resolveMealPlanDayItems(date: LocalDate): List<String> {
        return resolveMealPlanDayItems(date, mealPlanRepository, recipeRepository)
    }

    fun addRecipeToMealPlan(recipeId: String, dayDate: LocalDate) {
        viewModelScope.launch {
            addRecipeToMealPlan(recipeId, dayDate, mealPlanRepository)
        }
    }
}
