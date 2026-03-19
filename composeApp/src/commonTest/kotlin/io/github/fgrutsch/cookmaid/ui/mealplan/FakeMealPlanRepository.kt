package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class FakeMealPlanRepository : MealPlanRepository {

    val items: MutableList<MealPlanItem> = mutableListOf()

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem> {
        return items.filter { it.day in from..to }
    }

    override suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem {
        val item = if (recipeId != null) {
            MealPlanItem.Recipe(id = Uuid.random(), day = day, recipeId = recipeId, recipeName = "Recipe")
        } else {
            MealPlanItem.Note(id = Uuid.random(), day = day, name = note ?: "")
        }
        items.add(item)
        return item
    }

    override suspend fun update(id: Uuid, day: LocalDate?, note: String?) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = items[index]
            if (existing is MealPlanItem.Note && note != null) {
                items[index] = existing.copy(name = note, day = day ?: existing.day)
            }
        }
    }

    override suspend fun delete(id: Uuid) {
        items.removeAll { it.id == id }
    }
}
