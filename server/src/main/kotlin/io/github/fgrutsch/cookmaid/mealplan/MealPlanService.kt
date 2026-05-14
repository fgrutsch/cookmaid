package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * Orchestrates meal plan CRUD operations with ownership checks.
 */
class MealPlanService(
    private val recipeRepository: RecipeRepository,
    private val repository: MealPlanRepository,
) {

    private val logger = KotlinLogging.logger {}

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
        require((recipeId != null) xor (note != null)) {
            "Exactly one of recipeId or note must be provided"
        }
        if (recipeId != null && !recipeRepository.isOwner(userId, recipeId)) {
            logger.debug { "Recipe ownership check failed: userId=$userId, recipeId=$recipeId" }
            return null
        }
        val item = repository.create(userId, day, recipeId, note)
        logger.info { "Meal plan item created: userId=$userId, itemId=${item.id}, day=$day" }
        return item
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
        if (!repository.isOwner(userId, itemId)) {
            logger.debug { "Ownership check failed: userId=$userId, itemId=$itemId" }
            return false
        }
        repository.update(itemId, day, note)
        logger.info { "Meal plan item updated: userId=$userId, itemId=$itemId" }
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
        if (!repository.isOwner(userId, itemId)) {
            logger.debug { "Ownership check failed: userId=$userId, itemId=$itemId" }
            return false
        }
        repository.delete(itemId)
        logger.info { "Meal plan item deleted: userId=$userId, itemId=$itemId" }
        return true
    }
}
