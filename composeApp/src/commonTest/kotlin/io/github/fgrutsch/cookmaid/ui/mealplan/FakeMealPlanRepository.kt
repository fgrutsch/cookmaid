package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanItemResponse
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class FakeMealPlanRepository : MealPlanRepository {

    val items: MutableList<MealPlanItemResponse> = mutableListOf()

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItemResponse> {
        return items.filter { it.day in from..to }
    }

    override suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse {
        val item = MealPlanItemResponse(
            id = Uuid.random(),
            day = day,
            recipeId = recipeId,
            recipeName = if (recipeId != null) "Recipe" else null,
            note = note,
        )
        items.add(item)
        return item
    }

    override suspend fun update(id: Uuid, day: LocalDate?, note: String?) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = items[index]
            items[index] = existing.copy(
                day = day ?: existing.day,
                note = note ?: existing.note,
            )
        }
    }

    override suspend fun delete(id: Uuid) {
        items.removeAll { it.id == id }
    }
}
