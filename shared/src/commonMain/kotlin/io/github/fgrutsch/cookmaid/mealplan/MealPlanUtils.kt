package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

fun mondayOfWeek(date: LocalDate): LocalDate {
    val daysSinceMonday = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7
    return date.minus(daysSinceMonday, DateTimeUnit.DAY)
}
