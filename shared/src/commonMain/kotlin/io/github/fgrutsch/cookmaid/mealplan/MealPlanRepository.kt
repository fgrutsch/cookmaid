package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

interface MealPlanRepository {
    val weeks: StateFlow<List<MealPlanWeek>>
    suspend fun getOrCreateWeek(startDate: LocalDate): MealPlanWeek
    suspend fun addItem(weekStartDate: LocalDate, dayDate: LocalDate, item: MealPlanItem)
    suspend fun updateItem(weekStartDate: LocalDate, dayDate: LocalDate, itemId: Uuid, newItem: MealPlanItem)
    suspend fun removeItem(weekStartDate: LocalDate, dayDate: LocalDate, itemId: Uuid)
}

class InMemoryMealPlanRepository : MealPlanRepository {
    private val _weeks = MutableStateFlow(defaultWeeks())
    override val weeks: StateFlow<List<MealPlanWeek>> = _weeks.asStateFlow()

    override suspend fun getOrCreateWeek(startDate: LocalDate): MealPlanWeek {
        val existing = _weeks.value.find { it.startDate == startDate }
        if (existing != null) return existing
        val newWeek = createEmptyWeek(startDate)
        _weeks.update { it + newWeek }
        return newWeek
    }

    override suspend fun addItem(weekStartDate: LocalDate, dayDate: LocalDate, item: MealPlanItem) {
        _weeks.update { weeks ->
            weeks.map { week ->
                if (week.startDate == weekStartDate) {
                    week.copy(days = week.days.map { day ->
                        if (day.date == dayDate) day.copy(items = day.items + item)
                        else day
                    })
                } else week
            }
        }
    }

    override suspend fun updateItem(weekStartDate: LocalDate, dayDate: LocalDate, itemId: Uuid, newItem: MealPlanItem) {
        _weeks.update { weeks ->
            weeks.map { week ->
                if (week.startDate == weekStartDate) {
                    week.copy(days = week.days.map { day ->
                        if (day.date == dayDate) day.copy(items = day.items.map { if (it.id == itemId) newItem else it })
                        else day
                    })
                } else week
            }
        }
    }

    override suspend fun removeItem(weekStartDate: LocalDate, dayDate: LocalDate, itemId: Uuid) {
        _weeks.update { weeks ->
            weeks.map { week ->
                if (week.startDate == weekStartDate) {
                    week.copy(days = week.days.map { day ->
                        if (day.date == dayDate) day.copy(items = day.items.filter { it.id != itemId })
                        else day
                    })
                } else week
            }
        }
    }
}

fun mondayOfWeek(date: LocalDate): LocalDate {
    val daysSinceMonday = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7
    return date.minus(daysSinceMonday, DateTimeUnit.DAY)
}

fun createEmptyWeek(startDate: LocalDate): MealPlanWeek {
    val days = (0..6).map { offset ->
        MealPlanDay(date = startDate.plus(offset, DateTimeUnit.DAY))
    }
    return MealPlanWeek(startDate = startDate, days = days)
}

private fun defaultWeeks(): List<MealPlanWeek> {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val monday = mondayOfWeek(today)
    val week = MealPlanWeek(
        startDate = monday,
        days = (0..6).map { offset ->
            val date = monday.plus(offset, DateTimeUnit.DAY)
            when (offset) {
                0 -> MealPlanDay(date, listOf(
                    MealPlanItem.NoteItem(id = Uuid.random(), name = "Quick Sandwich"),
                ))
                else -> MealPlanDay(date)
            }
        },
    )
    return listOf(week)
}
