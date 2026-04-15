package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.user.UserId
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * Orchestrates meal plan CRUD operations with ownership checks.
 */
class MealPlanService(
    private val recipeRepository: RecipeRepository,
    private val repository: MealPlanRepository,
) {

    /**
     * Returns meal plan items for [userId] within the given date range.
     *
     * @param userId the owner of the meal plan.
     * @param from start date (inclusive).
     * @param to end date (inclusive).
     * @return the meal plan items in the range.
     */
    suspend fun find(userId: UserId, from: LocalDate, to: LocalDate): List<MealPlanItem> {
        return repository.find(userId, from, to)
    }

    /**
     * Creates a new meal plan item for [userId].
     *
     * @param userId the owner of the meal plan.
     * @param day the date for the item.
     * @param recipeId optional recipe reference; must belong to [userId] if non-null.
     * @param note optional free-text note.
     * @return the created meal plan item, or null if [recipeId] does not belong to [userId].
     */
    suspend fun create(userId: UserId, day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem? {
        if (recipeId != null && !recipeRepository.isOwner(userId, recipeId)) return null
        return repository.create(userId, day, recipeId, note)
    }

    /**
     * Updates a meal plan item if owned by [userId].
     *
     * @param userId the expected owner.
     * @param itemId the item to update.
     * @param day optional new date.
     * @param note optional new note.
     * @return true if the update succeeded, false if not found or not owned.
     */
    suspend fun update(userId: UserId, itemId: Uuid, day: LocalDate?, note: String?): Boolean {
        if (!repository.isOwner(userId, itemId)) return false
        repository.update(itemId, day, note)
        return true
    }

    /**
     * Deletes a meal plan item if owned by [userId].
     *
     * @param userId the expected owner.
     * @param itemId the item to delete.
     * @return true if the deletion succeeded, false if not found or not owned.
     */
    suspend fun delete(userId: UserId, itemId: Uuid): Boolean {
        if (!repository.isOwner(userId, itemId)) return false
        repository.delete(itemId)
        return true
    }
}
