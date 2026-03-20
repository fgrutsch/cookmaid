package io.github.fgrutsch.cookmaid.ui.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

fun formatShortDate(date: LocalDate): String {
    val monthName = when (date.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
    }
    return "$monthName ${date.day}"
}
