package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.user.UserId
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

class MealPlanService(
    private val repository: MealPlanRepository,
) {

    suspend fun find(userId: UserId, from: LocalDate, to: LocalDate): List<MealPlanItem> {
        return repository.find(userId, from, to)
    }

    suspend fun create(userId: UserId, day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem {
        return repository.create(userId, day, recipeId, note)
    }

    suspend fun update(userId: UserId, itemId: Uuid, day: LocalDate?, note: String?): Boolean {
        if (!repository.isOwner(userId, itemId)) return false
        repository.update(itemId, day, note)
        return true
    }

    suspend fun delete(userId: UserId, itemId: Uuid): Boolean {
        if (!repository.isOwner(userId, itemId)) return false
        repository.delete(itemId)
        return true
    }
}
