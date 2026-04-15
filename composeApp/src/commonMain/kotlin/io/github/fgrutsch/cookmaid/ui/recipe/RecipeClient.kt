package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.recipe.CreateRecipeRequest
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import io.github.fgrutsch.cookmaid.recipe.TagsResponse
import io.github.fgrutsch.cookmaid.recipe.UpdateRecipeRequest
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

/**
 * HTTP client for recipe endpoints. Interface-based so repository tests
 * can substitute a fake without constructing the full [ApiClient].
 */
interface RecipeClient {
    suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage
    suspend fun fetchById(id: Uuid): Recipe
    suspend fun fetchTags(): List<String>
    suspend fun create(request: CreateRecipeRequest): Recipe
    suspend fun update(id: Uuid, request: UpdateRecipeRequest)
    suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe
    suspend fun delete(id: Uuid)
}

class ApiRecipeClient(
    private val apiClient: ApiClient,
) : RecipeClient {
    private val base = "/api/recipes"

    override suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage =
        apiClient.httpClient.get(base) {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
            search?.let { parameter("search", it) }
            tag?.let { parameter("tag", it) }
        }.body()

    override suspend fun fetchById(id: Uuid): Recipe =
        apiClient.httpClient.get("$base/$id").body()

    override suspend fun fetchTags(): List<String> =
        apiClient.httpClient.get("$base/tags").body<TagsResponse>().items

    override suspend fun create(request: CreateRecipeRequest): Recipe =
        apiClient.httpClient.post(base) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun update(id: Uuid, request: UpdateRecipeRequest) {
        apiClient.httpClient.put("$base/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe =
        apiClient.httpClient.get("$base/random") {
            tag?.let { parameter("tag", it) }
            excludeId?.let { parameter("excludeId", it) }
        }.body()

    override suspend fun delete(id: Uuid) {
        apiClient.httpClient.delete("$base/$id")
    }
}
