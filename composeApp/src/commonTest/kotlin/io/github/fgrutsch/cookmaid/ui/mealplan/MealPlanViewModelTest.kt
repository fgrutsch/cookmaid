package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.mondayOfWeek
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import io.github.fgrutsch.cookmaid.ui.recipe.FakeRecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.FakeShoppingListRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class MealPlanViewModelTest : BaseViewModelTest() {

    private val fakeMealPlan = FakeMealPlanRepository()
    private val fakeRecipe = FakeRecipeRepository()
    private val fakeShopping = FakeShoppingListRepository()

    private fun createViewModel() = MealPlanViewModel(fakeMealPlan, fakeRecipe, fakeShopping)

    @Test
    fun `resetState returns state to initial with current week start`() = viewModelTest {
        val viewModel = createViewModel()
        viewModel.onEvent(MealPlanEvent.Load)
        advanceUntilIdle()
        // Navigate a couple of weeks back so currentWeekStart differs from today's.
        viewModel.onEvent(MealPlanEvent.PreviousWeek)
        viewModel.onEvent(MealPlanEvent.PreviousWeek)
        advanceUntilIdle()
        val navigatedWeek = viewModel.state.value.currentWeekStart
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val currentMonday = mondayOfWeek(today)
        // Sanity: we are not on the current week anymore.
        assertTrue(navigatedWeek < currentMonday)

        viewModel.resetState()

        val s = viewModel.state.value
        assertEquals(currentMonday, s.currentWeekStart)
        assertFalse(s.initialized)
        assertFalse(s.isLoading)
        assertTrue(s.days.isEmpty())
        assertTrue(s.recipeSearchResults.isEmpty())
    }

    @Test
    fun `resetState is a no-op on a fresh viewmodel`() = viewModelTest {
        val viewModel = createViewModel()
        val initial = viewModel.state.value

        viewModel.resetState()

        assertEquals(initial, viewModel.state.value)
    }
}
