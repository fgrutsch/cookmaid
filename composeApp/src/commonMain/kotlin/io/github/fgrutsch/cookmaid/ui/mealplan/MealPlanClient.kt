package io.github.fgrutsch.cookmaid.ui.mealplan

import io.github.fgrutsch.cookmaid.mealplan.CreateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.UpdateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

interface MealPlanClient {
    suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem>
    suspend fun create(request: CreateMealPlanItemRequest): MealPlanItem
    suspend fun update(id: Uuid, request: UpdateMealPlanItemRequest)
    suspend fun delete(id: Uuid)
}

class ApiMealPlanClient(
    private val apiClient: ApiClient,
) : MealPlanClient {
    private val base = "/api/meal-plan"

    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem> =
        apiClient.httpClient.get(base) {
            parameter("from", from.toString())
            parameter("to", to.toString())
        }.body()

    override suspend fun create(request: CreateMealPlanItemRequest): MealPlanItem =
        apiClient.httpClient.post(base) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(id: Uuid, request: UpdateMealPlanItemRequest) {
        apiClient.httpClient.put("$base/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun delete(id: Uuid) {
        apiClient.httpClient.delete("$base/$id")
    }
}
