package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.runtime.Composable
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.date_short
import cookmaid.composeapp.generated.resources.month_apr
import cookmaid.composeapp.generated.resources.month_aug
import cookmaid.composeapp.generated.resources.month_dec
import cookmaid.composeapp.generated.resources.month_feb
import cookmaid.composeapp.generated.resources.month_jan
import cookmaid.composeapp.generated.resources.month_jul
import cookmaid.composeapp.generated.resources.month_jun
import cookmaid.composeapp.generated.resources.month_mar
import cookmaid.composeapp.generated.resources.month_may
import cookmaid.composeapp.generated.resources.month_nov
import cookmaid.composeapp.generated.resources.month_oct
import cookmaid.composeapp.generated.resources.month_sep
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

@Composable
fun formatShortDate(date: LocalDate): String {
    val monthName = when (date.month) {
        Month.JANUARY -> Res.string.month_jan.resolve()
        Month.FEBRUARY -> Res.string.month_feb.resolve()
        Month.MARCH -> Res.string.month_mar.resolve()
        Month.APRIL -> Res.string.month_apr.resolve()
        Month.MAY -> Res.string.month_may.resolve()
        Month.JUNE -> Res.string.month_jun.resolve()
        Month.JULY -> Res.string.month_jul.resolve()
        Month.AUGUST -> Res.string.month_aug.resolve()
        Month.SEPTEMBER -> Res.string.month_sep.resolve()
        Month.OCTOBER -> Res.string.month_oct.resolve()
        Month.NOVEMBER -> Res.string.month_nov.resolve()
        Month.DECEMBER -> Res.string.month_dec.resolve()
    }
    return Res.string.date_short.resolve(monthName, date.day)
}
