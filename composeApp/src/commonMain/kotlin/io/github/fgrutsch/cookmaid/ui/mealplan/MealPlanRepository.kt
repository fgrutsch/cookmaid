package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.CreateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItemResponse
import io.github.fgrutsch.cookmaid.mealplan.UpdateMealPlanItemRequest
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

interface MealPlanRepository {
    suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItemResponse>
    suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse
    suspend fun update(id: Uuid, day: LocalDate?, note: String?)
    suspend fun delete(id: Uuid)
}

class ApiMealPlanRepository(
    private val client: MealPlanClient,
) : MealPlanRepository {

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItemResponse> {
        return client.fetchItems(from, to)
    }

    override suspend fun create(day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItemResponse {
        return client.create(CreateMealPlanItemRequest(day, recipeId, note))
    }

    override suspend fun update(id: Uuid, day: LocalDate?, note: String?) {
        client.update(id, UpdateMealPlanItemRequest(day, note))
    }

    override suspend fun delete(id: Uuid) {
        client.delete(id)
    }
}
