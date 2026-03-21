package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.CreateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.UpdateMealPlanItemRequest
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * Repository for meal plan CRUD operations.
 */
interface MealPlanRepository {
    /**
     * Fetches meal plan items within the given date range.
     *
     * @param from the start date (inclusive).
     * @param to the end date (inclusive).
     * @return list of [MealPlanItem] entries in the range.
     */
    suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem>

    /**
     * Creates a new meal plan item for the given [day].
     *
     * @param day the date for the meal plan entry.
     * @param recipeId optional recipe to link.
     * @param note optional free-text note.
     * @return the newly created [MealPlanItem].
     */
    suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem

    /**
     * Updates an existing meal plan item.
     *
     * @param id the unique meal plan item identifier.
     * @param day optional new date for the entry.
     * @param note optional updated note.
     */
    suspend fun update(id: Uuid, day: LocalDate?, note: String?)

    /**
     * Deletes the meal plan item with the given [id].
     *
     * @param id the unique meal plan item identifier.
     */
    suspend fun delete(id: Uuid)
}

class ApiMealPlanRepository(
    private val client: MealPlanClient,
) : MealPlanRepository {

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem> {
        return client.fetchItems(from, to)
    }

    override suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem {
        return client.create(CreateMealPlanItemRequest(day, recipeId, note))
    }

    override suspend fun update(id: Uuid, day: LocalDate?, note: String?) {
        client.update(id, UpdateMealPlanItemRequest(day, note))
    }

    override suspend fun delete(id: Uuid) {
        client.delete(id)
    }
}
