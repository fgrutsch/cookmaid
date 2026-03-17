package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.recipe.CreateRecipeRequest
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import io.github.fgrutsch.cookmaid.recipe.UpdateRecipeRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlin.uuid.Uuid

interface RecipeRepository {
    suspend fun fetchPage(cursor: String?, limit: Int = 20, search: String? = null, tag: String? = null): RecipePage
    suspend fun fetchTags(): List<String>
    suspend fun getById(id: Uuid): Recipe?
    suspend fun create(
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    ): Recipe
    suspend fun update(
        id: Uuid,
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    )
    suspend fun delete(id: Uuid)
}

class ApiRecipeRepository(
    private val client: RecipeClient,
) : RecipeRepository {

    override suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage {
        return client.fetchPage(cursor, limit, search, tag)
    }

    override suspend fun fetchTags(): List<String> {
        return client.fetchTags()
    }

    override suspend fun getById(id: Uuid): Recipe? {
        return try {
            client.fetchById(id)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }
    }

    override suspend fun create(
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    ): Recipe {
        return client.create(CreateRecipeRequest(name, ingredients, steps, tags))
    }

    override suspend fun update(
        id: Uuid,
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    ) {
        client.update(id, UpdateRecipeRequest(name, ingredients, steps, tags))
    }

    override suspend fun delete(id: Uuid) {
        client.delete(id)
    }
}
