package io.github.fgrutsch.cookmaid.ui.common

import kotlinx.datetime.LocalDate

fun formatShortDate(date: LocalDate): String {
    val monthName = when (date.month) {
        kotlinx.datetime.Month.JANUARY -> "Jan"
        kotlinx.datetime.Month.FEBRUARY -> "Feb"
        kotlinx.datetime.Month.MARCH -> "Mar"
        kotlinx.datetime.Month.APRIL -> "Apr"
        kotlinx.datetime.Month.MAY -> "May"
        kotlinx.datetime.Month.JUNE -> "Jun"
        kotlinx.datetime.Month.JULY -> "Jul"
        kotlinx.datetime.Month.AUGUST -> "Aug"
        kotlinx.datetime.Month.SEPTEMBER -> "Sep"
        kotlinx.datetime.Month.OCTOBER -> "Oct"
        kotlinx.datetime.Month.NOVEMBER -> "Nov"
        kotlinx.datetime.Month.DECEMBER -> "Dec"
    }
    return "$monthName ${date.day}"
}
