package io.github.fgrutsch.cookmaid.ui.recipe.edit

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.recipe.FakeRecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.FakeCatalogItemRepository
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AddRecipeViewModelTest : BaseViewModelTest() {

    private val fakeRecipeRepo = FakeRecipeRepository()
    private val fakeCatalog = FakeCatalogItemRepository()

    private fun TestScope.createViewModel(editRecipeId: Uuid? = null): AddRecipeViewModel {
        val viewModel = AddRecipeViewModel(fakeRecipeRepo, fakeCatalog, editRecipeId)
        viewModel.onEvent(AddRecipeEvent.Load)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `load populates available tags`() = viewModelTest {
        fakeRecipeRepo.tags = listOf("Italian", "Mexican")
        val viewModel = createViewModel()

        assertEquals(listOf("Italian", "Mexican"), viewModel.state.value.availableTags)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `load in edit mode populates recipe data`() = viewModelTest {
        val recipe = Recipe(
            id = Uuid.random(),
            name = "Pasta",
            ingredients = listOf(RecipeIngredient(item = Item.FreeTextItem("Tomato"), quantity = 2f)),
            steps = listOf("Boil water", "Cook pasta"),
            tags = listOf("Italian"),
        )
        fakeRecipeRepo.recipes.add(recipe)
        fakeRecipeRepo.tags = listOf("Italian", "Mexican")

        val viewModel = createViewModel(editRecipeId = recipe.id)

        assertTrue(viewModel.state.value.isEditing)
        assertEquals("Pasta", viewModel.state.value.name)
        assertEquals(1, viewModel.state.value.ingredients.size)
        assertEquals(2, viewModel.state.value.steps.size)
        assertEquals(listOf("Italian"), viewModel.state.value.selectedTags)
    }

    @Test
    fun `set name clears error`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.Save)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.nameError)

        viewModel.onEvent(AddRecipeEvent.SetName("Pasta"))
        advanceUntilIdle()

        assertEquals("Pasta", viewModel.state.value.name)
        assertFalse(viewModel.state.value.nameError)
    }

    @Test
    fun `add ingredient appends to list`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddIngredient(Item.FreeTextItem("Tomato"), 3f))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.ingredients.size)
        assertEquals("Tomato", viewModel.state.value.ingredients.first().item.name)
        assertEquals(3f, viewModel.state.value.ingredients.first().quantity)
    }

    @Test
    fun `add blank ingredient is ignored`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddIngredient(Item.FreeTextItem("  "), null))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.ingredients.isEmpty())
    }

    @Test
    fun `remove ingredient by index`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddIngredient(Item.FreeTextItem("Tomato"), null))
        viewModel.onEvent(AddRecipeEvent.AddIngredient(Item.FreeTextItem("Onion"), null))
        advanceUntilIdle()

        viewModel.onEvent(AddRecipeEvent.RemoveIngredient(0))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.ingredients.size)
        assertEquals("Onion", viewModel.state.value.ingredients.first().item.name)
    }

    @Test
    fun `update ingredient quantity`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddIngredient(Item.FreeTextItem("Tomato"), null))
        advanceUntilIdle()

        viewModel.onEvent(AddRecipeEvent.UpdateIngredientQuantity(0, 5f))
        advanceUntilIdle()

        assertEquals(5f, viewModel.state.value.ingredients.first().quantity)
    }

    @Test
    fun `add step appends to list`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddStep("Boil water"))
        advanceUntilIdle()

        assertEquals(listOf("Boil water"), viewModel.state.value.steps)
    }

    @Test
    fun `add blank step is ignored`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddStep("  "))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.steps.isEmpty())
    }

    @Test
    fun `remove step by index`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.AddStep("Boil water"))
        viewModel.onEvent(AddRecipeEvent.AddStep("Cook pasta"))
        advanceUntilIdle()

        viewModel.onEvent(AddRecipeEvent.RemoveStep(0))
        advanceUntilIdle()

        assertEquals(listOf("Cook pasta"), viewModel.state.value.steps)
    }

    @Test
    fun `toggle tag selects and deselects`() = viewModelTest {
        fakeRecipeRepo.tags = listOf("Italian")
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.ToggleTag("Italian"))
        advanceUntilIdle()
        assertEquals(listOf("Italian"), viewModel.state.value.selectedTags)

        viewModel.onEvent(AddRecipeEvent.ToggleTag("Italian"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.selectedTags.isEmpty())
    }

    @Test
    fun `create and add tag adds to both lists`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.CreateAndAddTag("Dessert"))
        advanceUntilIdle()

        assertTrue("Dessert" in viewModel.state.value.availableTags)
        assertTrue("Dessert" in viewModel.state.value.selectedTags)
    }

    @Test
    fun `create blank tag is ignored`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.CreateAndAddTag("  "))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.availableTags.isEmpty())
    }

    @Test
    fun `save with blank name shows error`() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(AddRecipeEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.nameError)
    }

    @Test
    fun `save creates recipe and sends Saved effect`() = viewModelTest {
        val viewModel = createViewModel()

        var effect: AddRecipeEffect? = null
        val job = launch {
            viewModel.effects.collect { effect = it }
        }

        viewModel.onEvent(AddRecipeEvent.SetName("Pasta"))
        viewModel.onEvent(AddRecipeEvent.AddStep("Cook"))
        viewModel.onEvent(AddRecipeEvent.Save)
        advanceUntilIdle()

        assertEquals(AddRecipeEffect.Saved, effect)
        assertEquals(1, fakeRecipeRepo.recipes.size)
        assertEquals("Pasta", fakeRecipeRepo.recipes.first().name)
        job.cancel()
    }

    @Test
    fun `save in edit mode updates existing recipe`() = viewModelTest {
        val recipe = Recipe(
            id = Uuid.random(), name = "Pasta",
            ingredients = emptyList(), steps = listOf("Cook"), tags = emptyList(),
        )
        fakeRecipeRepo.recipes.add(recipe)

        val viewModel = createViewModel(editRecipeId = recipe.id)

        var effect: AddRecipeEffect? = null
        val job = launch {
            viewModel.effects.collect { effect = it }
        }

        viewModel.onEvent(AddRecipeEvent.SetName("Updated Pasta"))
        viewModel.onEvent(AddRecipeEvent.Save)
        advanceUntilIdle()

        assertEquals(AddRecipeEffect.Saved, effect)
        assertEquals("Updated Pasta", fakeRecipeRepo.recipes.first().name)
        assertEquals(1, fakeRecipeRepo.recipes.size)
        job.cancel()
    }
}
