package io.github.fgrutsch.cookmaid.ui.common

import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanRepository
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

suspend fun addRecipeToMealPlan(
    recipeId: Uuid,
    day: LocalDate,
    mealPlanRepository: MealPlanRepository,
) {
    mealPlanRepository.create(day, recipeId = recipeId, note = null)
}
