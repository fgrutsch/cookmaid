package io.github.fgrutsch.mealplan

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class MealPlanDay(
    val date: LocalDate,
    val items: List<MealPlanItem> = emptyList(),
)

@Serializable
data class MealPlanWeek(
    val startDate: LocalDate,
    val days: List<MealPlanDay>,
)
