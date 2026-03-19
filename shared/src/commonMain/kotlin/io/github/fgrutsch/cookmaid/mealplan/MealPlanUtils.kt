package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val DAYS_IN_WEEK = 7

fun mondayOfWeek(date: LocalDate): LocalDate {
    val daysSinceMonday = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + DAYS_IN_WEEK) % DAYS_IN_WEEK
    return date.minus(daysSinceMonday, DateTimeUnit.DAY)
}
