package io.github.fgrutsch.cookmaid.ui.recipe.detail

import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.common.addIngredientsToDefaultShoppingList
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListRepository
import kotlin.uuid.Uuid

class RecipeDetailViewModel(
    private val recipeId: Uuid,
    private val recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : MviViewModel<RecipeDetailState, RecipeDetailEvent, RecipeDetailEffect>(RecipeDetailState()) {

    override fun handleEvent(event: RecipeDetailEvent) {
        when (event) {
            is RecipeDetailEvent.Load -> loadRecipe()
            is RecipeDetailEvent.Delete -> deleteRecipe()
            is RecipeDetailEvent.AddIngredientsToShoppingList -> addToShoppingList(event)
        }
    }

    private fun loadRecipe() {
        launch {
            updateState { copy(isLoading = true) }
            val recipe = recipeRepository.getById(recipeId)
            updateState { copy(recipe = recipe, isLoading = false) }
        }
    }

    private fun deleteRecipe() {
        launch {
            state.value.recipe?.let { recipeRepository.delete(it.id) }
            sendEffect(RecipeDetailEffect.Deleted)
        }
    }

    private fun addToShoppingList(event: RecipeDetailEvent.AddIngredientsToShoppingList) {
        launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, event.ingredients)
            sendEffect(RecipeDetailEffect.AddedToShoppingList)
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false) }
        sendEffect(RecipeDetailEffect.Error("Something went wrong. Please try again."))
    }
}
