package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

const val DAYS_IN_WEEK = 7
const val WEEK_END_OFFSET = 6

fun mondayOfWeek(date: LocalDate): LocalDate {
    val daysSinceMonday = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + DAYS_IN_WEEK) % DAYS_IN_WEEK
    return date.minus(daysSinceMonday, DateTimeUnit.DAY)
}
