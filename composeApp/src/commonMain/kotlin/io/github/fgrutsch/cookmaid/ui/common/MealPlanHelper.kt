package io.github.fgrutsch.cookmaid.ui.common

import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanRepository
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

suspend fun addRecipeToMealPlan(
    recipeId: Uuid,
    dayDate: LocalDate,
    mealPlanRepository: MealPlanRepository,
) {
    mealPlanRepository.create(dayDate, recipeId = recipeId, note = null)
}
