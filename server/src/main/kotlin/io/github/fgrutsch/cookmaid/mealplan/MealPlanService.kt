package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class MealPlanService(
    private val repository: ServerMealPlanRepository,
) {

    suspend fun findByUser(userId: Uuid, from: LocalDate, to: LocalDate): List<MealPlanItemResponse> {
        return repository.findByUserAndDateRange(userId, from, to)
    }

    suspend fun create(userId: Uuid, dayDate: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse {
        return repository.create(userId, dayDate, recipeId, note)
    }

    suspend fun update(userId: Uuid, itemId: Uuid, dayDate: LocalDate?, note: String?): Boolean {
        if (!repository.isOwnedByUser(userId, itemId)) return false
        repository.update(itemId, dayDate, note)
        return true
    }

    suspend fun delete(userId: Uuid, itemId: Uuid): Boolean {
        if (!repository.isOwnedByUser(userId, itemId)) return false
        repository.delete(itemId)
        return true
    }
}
