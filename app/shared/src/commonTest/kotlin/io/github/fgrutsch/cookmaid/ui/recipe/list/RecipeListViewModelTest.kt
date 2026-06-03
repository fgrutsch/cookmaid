package io.github.fgrutsch.cookmaid.ui.recipe.list

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.mealplan.FakeMealPlanRepository
import io.github.fgrutsch.cookmaid.ui.recipe.FakeRecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.FakeShoppingListRepository
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest : BaseViewModelTest() {

    private val fakeRecipeRepo = FakeRecipeRepository()
    private val fakeShoppingRepo = FakeShoppingListRepository()
    private val fakeMealPlanRepo = FakeMealPlanRepository()

    private fun TestScope.createLoadedViewModel(
        recipes: List<Recipe> = emptyList(),
        tags: List<String> = emptyList(),
    ): RecipeListViewModel {
        fakeRecipeRepo.recipes = recipes.toMutableList()
        fakeRecipeRepo.tags = tags
        val viewModel = RecipeListViewModel(fakeRecipeRepo, fakeShoppingRepo, fakeMealPlanRepo)
        viewModel.onEvent(RecipeListEvent.LoadRecipes)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `load recipes populates state`() = viewModelTest {
        val recipes = listOf(recipe("Pasta"), recipe("Salad"))
        val viewModel = createLoadedViewModel(recipes = recipes, tags = listOf("Italian"))

        assertEquals(2, viewModel.state.value.recipes.size)
        assertEquals(listOf("Italian"), viewModel.state.value.availableTags)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `load recipes with empty list`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        assertTrue(viewModel.state.value.recipes.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `second load does not show loading spinner`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))

        viewModel.onEvent(RecipeListEvent.LoadRecipes)
        // isLoading should not be set on second call
        assertFalse(viewModel.state.value.isLoading)
        advanceUntilIdle()
    }

    @Test
    fun `refresh reloads data with refresh flag`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))

        fakeRecipeRepo.recipes.add(recipe("Salad"))
        viewModel.onEvent(RecipeListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.recipes.size)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `select tag filters and deselects on second tap`() = viewModelTest {
        val viewModel = createLoadedViewModel(
            recipes = listOf(recipe("Pasta", tags = listOf("Italian")), recipe("Tacos", tags = listOf("Mexican"))),
            tags = listOf("Italian", "Mexican"),
        )

        viewModel.onEvent(RecipeListEvent.SelectTag("Italian"))
        advanceUntilIdle()

        assertEquals("Italian", viewModel.state.value.selectedTag)
        assertEquals(1, viewModel.state.value.recipes.size)
        assertEquals("Pasta", viewModel.state.value.recipes.first().name)

        viewModel.onEvent(RecipeListEvent.SelectTag("Italian"))
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedTag)
        assertEquals(2, viewModel.state.value.recipes.size)
    }

    @Test
    fun `delete recipe removes from list`() = viewModelTest {
        val r = recipe("Pasta")
        val viewModel = createLoadedViewModel(recipes = listOf(r, recipe("Salad")))

        viewModel.onEvent(RecipeListEvent.DeleteRecipe(r.id))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.recipes.size)
        assertEquals("Salad", viewModel.state.value.recipes.first().name)
    }

    @Test
    fun `roll random recipe sets randomRecipe`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta"), recipe("Salad")))

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.randomRecipe)
        assertFalse(viewModel.state.value.isLoadingRandom)
    }

    @Test
    fun `roll random recipe with empty list returns null`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()

        assertNull(viewModel.state.value.randomRecipe)
        assertFalse(viewModel.state.value.isLoadingRandom)
    }

    @Test
    fun `roll random recipe respects tag filter`() = viewModelTest {
        val viewModel = createLoadedViewModel(
            recipes = listOf(
                recipe("Pasta", tags = listOf("Italian")),
                recipe("Tacos", tags = listOf("Mexican")),
            ),
            tags = listOf("Italian", "Mexican"),
        )

        viewModel.onEvent(RecipeListEvent.SelectTag("Italian"))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()

        val random = viewModel.state.value.randomRecipe
        assertNotNull(random)
        assertTrue(random.tags.contains("Italian"))
    }

    @Test
    fun `clear random recipe resets state`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.randomRecipe)

        viewModel.onEvent(RecipeListEvent.ClearRandomRecipe)
        advanceUntilIdle()
        assertNull(viewModel.state.value.randomRecipe)
    }

    @Test
    fun `roll random recipe accumulates shownRecipeIds`() = viewModelTest {
        val viewModel = createLoadedViewModel(
            recipes = listOf(recipe("Pasta"), recipe("Salad"), recipe("Soup")),
        )

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()
        val first = viewModel.state.value.randomRecipe
        assertNotNull(first)
        assertEquals(1, viewModel.state.value.shownRecipeIds.size)
        assertTrue(viewModel.state.value.shownRecipeIds.contains(first.id.toString()))

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()
        val second = viewModel.state.value.randomRecipe
        assertNotNull(second)
        assertEquals(2, viewModel.state.value.shownRecipeIds.size)
        assertTrue(viewModel.state.value.shownRecipeIds.contains(first.id.toString()))
        assertTrue(viewModel.state.value.shownRecipeIds.contains(second.id.toString()))
    }

    @Test
    fun `clear random recipe resets shownRecipeIds`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta"), recipe("Salad")))

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.shownRecipeIds.isNotEmpty())

        viewModel.onEvent(RecipeListEvent.ClearRandomRecipe)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.shownRecipeIds.isEmpty())
    }

    @Test
    fun `select tag resets shownRecipeIds`() = viewModelTest {
        val viewModel = createLoadedViewModel(
            recipes = listOf(
                recipe("Pasta", tags = listOf("Italian")),
                recipe("Tacos", tags = listOf("Mexican")),
            ),
            tags = listOf("Italian", "Mexican"),
        )

        viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.shownRecipeIds.isNotEmpty())

        viewModel.onEvent(RecipeListEvent.SelectTag("Italian"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.shownRecipeIds.isEmpty())
    }

    @Test
    fun `set search active and close resets query`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))

        viewModel.onEvent(RecipeListEvent.SetSearchActive(true))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.searchActive)

        viewModel.onEvent(RecipeListEvent.SetSearchActive(false))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.searchActive)
        assertEquals("", viewModel.state.value.searchQuery)
    }

    @Test
    fun `closing search triggers exactly one recipe fetch`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))

        // Baseline: one fetch from the initial load.
        val baseline = fakeRecipeRepo.fetchPageCallCount

        viewModel.onEvent(RecipeListEvent.UpdateSearchQuery("pa"))
        advanceUntilIdle() // let the debounced search fetch resolve
        val afterSearch = fakeRecipeRepo.fetchPageCallCount

        viewModel.onEvent(RecipeListEvent.SetSearchActive(false))
        advanceUntilIdle() // flushes any lurking debounce timer as well

        val afterClose = fakeRecipeRepo.fetchPageCallCount - afterSearch
        assertEquals(1, afterClose)
        // Sanity check: typing did trigger a fetch between baseline and close.
        assertTrue(afterSearch > baseline)
    }

    @Test
    fun `typing search query triggers one fetch after debounce`() = viewModelTest {
        val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))
        val baseline = fakeRecipeRepo.fetchPageCallCount

        viewModel.onEvent(RecipeListEvent.UpdateSearchQuery("pa"))
        advanceUntilIdle()

        assertEquals(1, fakeRecipeRepo.fetchPageCallCount - baseline)
    }

    @Test
    fun `add ingredients to shopping list sends effect`() = viewModelTest {
        fakeShoppingRepo.lists = mutableListOf(ShoppingList(id = Uuid.random(), name = "Groceries", default = true))
        val viewModel = createLoadedViewModel()

        var effect: RecipeListEffect? = null
        val job = launch {
            viewModel.effects.collect { effect = it }
        }

        val ingredients = listOf(RecipeIngredient(item = Item.FreeText("Tomatoes"), quantity = "3"))
        viewModel.onEvent(RecipeListEvent.AddIngredientsToShoppingList(ingredients))
        advanceUntilIdle()

        assertEquals(RecipeListEffect.AddedToShoppingList, effect)
        job.cancel()
    }

    @Test
    fun `load more guards against concurrent calls`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        // hasMore is true by default but recipes are empty, so no cursor — loadMore should no-op
        viewModel.onEvent(RecipeListEvent.LoadMore)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoadingMore)
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
