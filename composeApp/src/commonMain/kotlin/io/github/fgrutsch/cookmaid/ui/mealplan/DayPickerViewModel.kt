package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.DAYS_IN_WEEK
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.WEEK_END_OFFSET
import io.github.fgrutsch.cookmaid.mealplan.mondayOfWeek
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class DayPickerViewModel(
    private val mealPlanRepository: MealPlanRepository,
) : MviViewModel<DayPickerState, DayPickerEvent, Nothing>(
    DayPickerState(weekStart = mondayOfWeek(Clock.System.todayIn(TimeZone.currentSystemDefault()))),
) {

    override fun handleEvent(event: DayPickerEvent) {
        when (event) {
            is DayPickerEvent.Load -> loadWeek()
            is DayPickerEvent.PreviousWeek -> navigateWeek(-DAYS_IN_WEEK)
            is DayPickerEvent.NextWeek -> navigateWeek(DAYS_IN_WEEK)
        }
    }

    private fun loadWeek() {
        launch {
            updateState { copy(isLoading = true) }
            val items = fetchWeekItems(state.value.weekStart)
            updateState { copy(itemsByDay = groupItemNames(items), isLoading = false) }
        }
    }

    private fun navigateWeek(dayOffset: Int) {
        val newStart = state.value.weekStart.plus(dayOffset, DateTimeUnit.DAY)
        updateState { copy(weekStart = newStart, isLoading = true) }
        launch {
            val items = fetchWeekItems(newStart)
            updateState { copy(itemsByDay = groupItemNames(items), isLoading = false) }
        }
    }

    private suspend fun fetchWeekItems(weekStart: LocalDate): List<MealPlanItem> {
        val weekEnd = weekStart.plus(WEEK_END_OFFSET, DateTimeUnit.DAY)
        return mealPlanRepository.fetchItems(weekStart, weekEnd)
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false) }
    }
}

data class DayPickerState(
    val weekStart: LocalDate,
    val itemsByDay: Map<LocalDate, List<String>> = emptyMap(),
    val isLoading: Boolean = true,
)

sealed interface DayPickerEvent {
    data object Load : DayPickerEvent
    data object PreviousWeek : DayPickerEvent
    data object NextWeek : DayPickerEvent
}

private fun groupItemNames(items: List<MealPlanItem>): Map<LocalDate, List<String>> {
    return items.groupBy(
        keySelector = { it.day },
        valueTransform = { item ->
            when (item) {
                is MealPlanItem.Recipe -> item.recipeName
                is MealPlanItem.Note -> item.name
            }
        },
    )
}
