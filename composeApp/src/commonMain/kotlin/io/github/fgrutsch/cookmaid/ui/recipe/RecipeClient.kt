package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import io.github.fgrutsch.cookmaid.recipe.TagsResponse
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
import kotlin.uuid.Uuid

class RecipeClient(
    private val apiClient: ApiClient,
) {
    private val base = "/api/recipes"

    suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage =
        apiClient.httpClient.get(base) {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
            search?.let { parameter("search", it) }
            tag?.let { parameter("tag", it) }
        }.body()

    suspend fun fetchById(id: Uuid): Recipe =
        apiClient.httpClient.get("$base/$id").body()

    suspend fun fetchTags(): List<String> =
        apiClient.httpClient.get("$base/tags").body<TagsResponse>().items

    suspend fun create(request: RecipeRequest): Recipe =
        apiClient.httpClient.post(base) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun update(id: Uuid, request: RecipeRequest) {
        apiClient.httpClient.put("$base/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe =
        apiClient.httpClient.get("$base/random") {
            tag?.let { parameter("tag", it) }
            excludeId?.let { parameter("excludeId", it) }
        }.body()

    suspend fun delete(id: Uuid) {
        apiClient.httpClient.delete("$base/$id")
    }
}
