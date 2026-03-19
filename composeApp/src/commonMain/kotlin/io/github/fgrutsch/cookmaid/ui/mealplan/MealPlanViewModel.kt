package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.DAYS_IN_WEEK
import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.WEEK_END_OFFSET
import io.github.fgrutsch.cookmaid.mealplan.mondayOfWeek
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.common.addIngredientsToDefaultShoppingList
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListRepository
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
) : MviViewModel<MealPlanState, MealPlanEvent, MealPlanEffect>(
    MealPlanState(currentWeekStart = mondayOfWeek(Clock.System.todayIn(TimeZone.currentSystemDefault()))),
) {

    override fun handleEvent(event: MealPlanEvent) {
        when (event) {
            is MealPlanEvent.Load -> loadWeek(showLoading = !state.value.initialized)
            is MealPlanEvent.Refresh -> loadWeek(showLoading = true)
            is MealPlanEvent.PreviousWeek -> navigateWeek(-DAYS_IN_WEEK)
            is MealPlanEvent.NextWeek -> navigateWeek(DAYS_IN_WEEK)
            is MealPlanEvent.GoToCurrentWeek -> goToCurrentWeek()
            is MealPlanEvent.SearchRecipes -> searchRecipes(event.query)
            is MealPlanEvent.AddRecipeItem -> addRecipeItem(event.day, event.recipeId)
            is MealPlanEvent.AddNoteItem -> addNoteItem(event.day, event.note)
            is MealPlanEvent.UpdateNote -> updateNote(event.itemId, event.day, event.newNote)
            is MealPlanEvent.DeleteItem -> deleteItem(event.itemId, event.day)
            is MealPlanEvent.AddRecipeToShoppingList -> addRecipeToShoppingList(event.recipeId, event.recipeName)
        }
    }

    private fun loadWeek(showLoading: Boolean) {
        launch {
            if (showLoading) updateState { copy(isLoading = true) }
            val items = fetchWeekItems(state.value.currentWeekStart)
            updateState {
                copy(
                    initialized = true,
                    days = groupIntoDays(currentWeekStart, items),
                    isLoading = false,
                )
            }
        }
    }

    private fun navigateWeek(dayOffset: Int) {
        val newStart = state.value.currentWeekStart.plus(dayOffset, DateTimeUnit.DAY)
        updateState { copy(currentWeekStart = newStart) }
        launch {
            val items = fetchWeekItems(newStart)
            updateState { copy(days = groupIntoDays(newStart, items)) }
        }
    }

    private fun goToCurrentWeek() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val newStart = mondayOfWeek(today)
        updateState { copy(currentWeekStart = newStart) }
        launch {
            val items = fetchWeekItems(newStart)
            updateState { copy(days = groupIntoDays(newStart, items)) }
        }
    }

    private fun searchRecipes(query: String) {
        launch {
            val search = query.takeIf { it.isNotBlank() }
            val page = recipeRepository.fetchPage(
                cursor = null,
                limit = RECIPE_SEARCH_LIMIT,
                search = search,
            )
            updateState { copy(recipeSearchResults = page.items) }
        }
    }

    private fun addRecipeItem(day: LocalDate, recipeId: Uuid) {
        launch {
            val created = mealPlanRepository.create(day, recipeId = recipeId, note = null)
            addItemToDay(day, created)
        }
    }

    private fun addNoteItem(day: LocalDate, note: String) {
        if (note.isBlank()) return
        launch {
            val created = mealPlanRepository.create(day, recipeId = null, note = note.trim())
            addItemToDay(day, created)
        }
    }

    private fun updateNote(itemId: Uuid, day: LocalDate, newNote: String) {
        if (newNote.isBlank()) return
        launch {
            mealPlanRepository.update(itemId, day = null, note = newNote.trim())
            updateState {
                copy(days = days.map { d ->
                    if (d.date == day) {
                        d.copy(items = d.items.map { item ->
                            if (item.id == itemId && item is MealPlanItem.Note) {
                                item.copy(name = newNote.trim())
                            } else item
                        })
                    } else d
                })
            }
        }
    }

    private fun deleteItem(itemId: Uuid, day: LocalDate) {
        launchOptimistic(
            optimisticUpdate = {
                copy(days = days.map { d ->
                    if (d.date == day) d.copy(items = d.items.filter { it.id != itemId })
                    else d
                })
            },
        ) {
            mealPlanRepository.delete(itemId)
        }
    }

    private fun addRecipeToShoppingList(recipeId: Uuid, recipeName: String) {
        launch {
            val recipe = recipeRepository.getById(recipeId)
            val ingredients = recipe?.ingredients.orEmpty()
            if (ingredients.isNotEmpty()) {
                sendEffect(MealPlanEffect.ShowIngredientPicker(recipeName, ingredients))
            }
        }
    }

    fun addIngredientsToShoppingList(ingredients: List<RecipeIngredient>) {
        launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
            sendEffect(MealPlanEffect.IngredientsAdded)
        }
    }

    private suspend fun fetchWeekItems(weekStart: LocalDate): List<MealPlanItem> {
        val weekEnd = weekStart.plus(WEEK_END_OFFSET, DateTimeUnit.DAY)
        return mealPlanRepository.fetchItems(weekStart, weekEnd)
    }

    private fun addItemToDay(day: LocalDate, item: MealPlanItem) {
        updateState {
            copy(days = days.map { d ->
                if (d.date == day) d.copy(items = d.items + item)
                else d
            })
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false) }
        sendEffect(MealPlanEffect.Error("Something went wrong. Please try again."))
    }

    companion object {
        private const val RECIPE_SEARCH_LIMIT = 20
    }
}

private fun groupIntoDays(weekStart: LocalDate, items: List<MealPlanItem>): List<MealPlanDay> {
    val itemsByDate = items.groupBy { it.day }
    return (0..WEEK_END_OFFSET).map { offset ->
        val date = weekStart.plus(offset, DateTimeUnit.DAY)
        MealPlanDay(date, itemsByDate[date].orEmpty())
    }
}
