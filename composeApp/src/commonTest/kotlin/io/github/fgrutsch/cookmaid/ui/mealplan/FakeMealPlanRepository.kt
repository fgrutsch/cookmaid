package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanItemResponse
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class FakeMealPlanRepository : MealPlanRepository {

    val items: MutableList<MealPlanItemResponse> = mutableListOf()

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItemResponse> {
        return items.filter { it.dayDate in from..to }
    }

    override suspend fun create(dayDate: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse {
        val item = MealPlanItemResponse(
            id = Uuid.random(),
            dayDate = dayDate,
            recipeId = recipeId,
            recipeName = if (recipeId != null) "Recipe" else null,
            note = note,
        )
        items.add(item)
        return item
    }

    override suspend fun update(id: Uuid, dayDate: LocalDate?, note: String?) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = items[index]
            items[index] = existing.copy(
                dayDate = dayDate ?: existing.dayDate,
                note = note ?: existing.note,
            )
        }
    }

    override suspend fun delete(id: Uuid) {
        items.removeAll { it.id == id }
    }
}
