package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

const val DAYS_IN_WEEK = 7
const val WEEK_END_OFFSET = 6

/**
 * Returns the Monday of the week that [date] falls in.
 *
 * @param date the date to find the Monday for.
 * @return the [LocalDate] of the Monday in the same ISO week as [date].
 */
fun mondayOfWeek(date: LocalDate): LocalDate {
    val daysSinceMonday = (date.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + DAYS_IN_WEEK) % DAYS_IN_WEEK
    return date.minus(daysSinceMonday, DateTimeUnit.DAY)
}
