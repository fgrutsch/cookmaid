package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
