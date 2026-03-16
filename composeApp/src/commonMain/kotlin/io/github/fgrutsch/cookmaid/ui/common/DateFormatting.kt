package io.github.fgrutsch.cookmaid.ui.common

import kotlinx.datetime.LocalDate

fun formatShortDate(date: LocalDate): String {
    val monthName = when (date.monthNumber) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> ""
    }
    return "$monthName ${date.dayOfMonth}"
}
