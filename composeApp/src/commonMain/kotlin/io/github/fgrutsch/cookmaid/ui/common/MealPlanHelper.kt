package io.github.fgrutsch.cookmaid.ui.common

import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.MealPlanRepository
import io.github.fgrutsch.cookmaid.mealplan.mondayOfWeek
import io.github.fgrutsch.cookmaid.recipe.RecipeRepository
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun resolveMealPlanDayItems(
    date: LocalDate,
    mealPlanRepository: MealPlanRepository,
    recipeRepository: RecipeRepository,
): List<String> {
    val weekStart = mondayOfWeek(date)
    val week = mealPlanRepository.weeks.value.find { it.startDate == weekStart }
    val day = week?.days?.find { it.date == date } ?: return emptyList()
    return day.items.map { item ->
        when (item) {
            is MealPlanItem.RecipeItem ->
                recipeRepository.recipes.value.find { it.id == item.recipeId }?.name ?: "Unknown recipe"
            is MealPlanItem.NoteItem -> item.name
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun addRecipeToMealPlan(
    recipeId: String,
    dayDate: LocalDate,
    mealPlanRepository: MealPlanRepository,
) {
    val weekStart = mondayOfWeek(dayDate)
    mealPlanRepository.getOrCreateWeek(weekStart)
    mealPlanRepository.addItem(
        weekStart,
        dayDate,
        MealPlanItem.RecipeItem(id = Uuid.random().toString(), recipeId = recipeId),
    )
}
