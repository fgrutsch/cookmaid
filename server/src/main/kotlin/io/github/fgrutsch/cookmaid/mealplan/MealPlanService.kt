package io.github.fgrutsch.cookmaid.mealplan

import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class MealPlanService(
    private val repository: MealPlanRepository,
) {

    suspend fun findByUser(userId: Uuid, from: LocalDate, to: LocalDate): List<MealPlanItemResponse> {
        return repository.findByUserAndDateRange(userId, from, to)
    }

    suspend fun create(userId: Uuid, day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse {
        return repository.create(userId, day, recipeId, note)
    }

    suspend fun update(userId: Uuid, itemId: Uuid, day: LocalDate?, note: String?): Boolean {
        if (!repository.isOwnedByUser(userId, itemId)) return false
        repository.update(itemId, day, note)
        return true
    }

    suspend fun delete(userId: Uuid, itemId: Uuid): Boolean {
        if (!repository.isOwnedByUser(userId, itemId)) return false
        repository.delete(itemId)
        return true
    }
}
