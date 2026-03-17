package io.github.fgrutsch.cookmaid.ui.recipe.detail

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.ui.recipe.FakeRecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.FakeShoppingListRepository
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest : BaseViewModelTest() {

    private val fakeRecipeRepo = FakeRecipeRepository()
    private val fakeShoppingRepo = FakeShoppingListRepository()

    @Test
    fun `load recipe populates state`() = viewModelTest {
        val recipe = recipe("Pasta")
        fakeRecipeRepo.recipes.add(recipe)

        val viewModel = RecipeDetailViewModel(recipe.id, fakeRecipeRepo, fakeShoppingRepo)
        viewModel.onEvent(RecipeDetailEvent.Load)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.recipe)
        assertEquals("Pasta", viewModel.state.value.recipe?.name)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `load non-existent recipe results in null`() = viewModelTest {
        val viewModel = RecipeDetailViewModel(Uuid.random(), fakeRecipeRepo, fakeShoppingRepo)
        viewModel.onEvent(RecipeDetailEvent.Load)
        advanceUntilIdle()

        assertNull(viewModel.state.value.recipe)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `delete recipe sends Deleted effect`() = viewModelTest {
        val recipe = recipe("Pasta")
        fakeRecipeRepo.recipes.add(recipe)

        val viewModel = RecipeDetailViewModel(recipe.id, fakeRecipeRepo, fakeShoppingRepo)
        viewModel.onEvent(RecipeDetailEvent.Load)
        advanceUntilIdle()

        var effect: RecipeDetailEffect? = null
        val job = launch {
            viewModel.effects.collect { effect = it }
        }

        viewModel.onEvent(RecipeDetailEvent.Delete)
        advanceUntilIdle()

        assertEquals(RecipeDetailEffect.Deleted, effect)
        assertTrue(fakeRecipeRepo.recipes.isEmpty())
        job.cancel()
    }

    @Test
    fun `add ingredients to shopping list sends effect`() = viewModelTest {
        fakeShoppingRepo.lists = mutableListOf(ShoppingList(id = Uuid.random(), name = "Groceries", default = true))
        val recipe = recipe("Pasta", ingredients = listOf(
            RecipeIngredient(item = Item.FreeTextItem("Tomatoes"), quantity = 3f),
        ))
        fakeRecipeRepo.recipes.add(recipe)

        val viewModel = RecipeDetailViewModel(recipe.id, fakeRecipeRepo, fakeShoppingRepo)
        viewModel.onEvent(RecipeDetailEvent.Load)
        advanceUntilIdle()

        var effect: RecipeDetailEffect? = null
        val job = launch {
            viewModel.effects.collect { effect = it }
        }

        viewModel.onEvent(RecipeDetailEvent.AddIngredientsToShoppingList(recipe.ingredients))
        advanceUntilIdle()

        assertEquals(RecipeDetailEffect.AddedToShoppingList, effect)
        job.cancel()
    }

    companion object {
        private fun recipe(
            name: String,
            ingredients: List<RecipeIngredient> = emptyList(),
            steps: List<String> = emptyList(),
            tags: List<String> = emptyList(),
        ) = Recipe(id = Uuid.random(), name = name, ingredients = ingredients, steps = steps, tags = tags)
    }
}
