package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItemResponse
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

    private var initialized = false

    override fun handleEvent(event: MealPlanEvent) {
        when (event) {
            is MealPlanEvent.Load -> load()
            is MealPlanEvent.PreviousWeek -> navigateWeek(-7)
            is MealPlanEvent.NextWeek -> navigateWeek(7)
            is MealPlanEvent.GoToCurrentWeek -> goToCurrentWeek()
            is MealPlanEvent.AddRecipeItem -> addRecipeItem(event.day, event.recipeId)
            is MealPlanEvent.AddNoteItem -> addNoteItem(event.day, event.note)
            is MealPlanEvent.UpdateNote -> updateNote(event.itemId, event.day, event.newNote)
            is MealPlanEvent.DeleteItem -> deleteItem(event.itemId, event.day)
            is MealPlanEvent.AddIngredientsToShoppingList -> addToShoppingList(event.ingredients)
        }
    }

    private fun load() {
        val firstLoad = !initialized
        initialized = true
        launch {
            if (firstLoad) updateState { copy(isLoading = true) }
            val recipes = recipeRepository.fetchPage(cursor = null, limit = 100).items
            val items = fetchWeekItems(state.value.currentWeekStart)
            updateState {
                copy(
                    recipes = recipes,
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
                copy(days = days.map { day ->
                    if (day.date == day) {
                        day.copy(items = day.items.map { item ->
                            if (item.id == itemId && item is MealPlanItem.Note) {
                                item.copy(name = newNote.trim())
                            } else item
                        })
                    } else day
                })
            }
        }
    }

    private fun deleteItem(itemId: Uuid, day: LocalDate) {
        launchOptimistic(
            optimisticUpdate = {
                copy(days = days.map { day ->
                    if (day.date == day) day.copy(items = day.items.filter { it.id != itemId })
                    else day
                })
            },
        ) {
            mealPlanRepository.delete(itemId)
        }
    }

    private fun addToShoppingList(ingredients: List<RecipeIngredient>) {
        launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
            sendEffect(MealPlanEffect.IngredientsAdded)
        }
    }

    fun resolveRecipeIngredients(recipeId: Uuid): List<RecipeIngredient> {
        return state.value.recipes.find { it.id == recipeId }?.ingredients ?: emptyList()
    }

    private suspend fun fetchWeekItems(weekStart: LocalDate): List<MealPlanItemResponse> {
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        return mealPlanRepository.fetchItems(weekStart, weekEnd)
    }

    private fun addItemToDay(day: LocalDate, response: MealPlanItemResponse) {
        val item = response.toMealPlanItem()
        updateState {
            copy(days = days.map { day ->
                if (day.date == day) day.copy(items = day.items + item)
                else day
            })
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false) }
        sendEffect(MealPlanEffect.Error("Something went wrong. Please try again."))
    }
}

private fun groupIntoDays(weekStart: LocalDate, items: List<MealPlanItemResponse>): List<MealPlanDay> {
    val itemsByDate = items.groupBy { it.day }
    return (0..6).map { offset ->
        val date = weekStart.plus(offset, DateTimeUnit.DAY)
        val dayItems = itemsByDate[date]?.map { it.toMealPlanItem() } ?: emptyList()
        MealPlanDay(date, dayItems)
    }
}

private fun MealPlanItemResponse.toMealPlanItem(): MealPlanItem {
    val rid = recipeId
    return if (rid != null) {
        MealPlanItem.Recipe(id = id, recipeId = rid, recipeName = recipeName ?: "Unknown recipe")
    } else {
        MealPlanItem.Note(id = id, name = note ?: "")
    }
}
