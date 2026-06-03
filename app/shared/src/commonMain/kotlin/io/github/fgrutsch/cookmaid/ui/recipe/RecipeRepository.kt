package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.recipe.DEFAULT_RECIPE_PAGE_SIZE
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlin.uuid.Uuid

/**
 * Repository for recipe CRUD operations and tag retrieval.
 */
interface RecipeRepository {
    /**
     * Returns a paginated page of recipes, optionally filtered by [search] text or [tag].
     *
     * @param cursor opaque cursor for the next page, or null for the first page.
     * @param limit maximum number of recipes per page.
     * @param search optional free-text search query.
     * @param tag optional tag to filter by.
     * @return a [RecipePage] containing the matching recipes and pagination info.
     */
    suspend fun fetchPage(
        cursor: String?,
        limit: Int = DEFAULT_RECIPE_PAGE_SIZE,
        search: String? = null,
        tag: String? = null,
    ): RecipePage

    /**
     * Returns all distinct tags used across recipes.
     *
     * @return list of tag strings.
     */
    suspend fun fetchTags(): List<String>

    /**
     * Returns the recipe with the given [id], or null if not found.
     *
     * @param id the unique recipe identifier.
     * @return the matching [Recipe], or null.
     */
    suspend fun getById(id: Uuid): Recipe?

    /**
     * Creates a new recipe and returns it.
     *
     * @param name the recipe name.
     * @param description optional recipe description.
     * @param ingredients the list of ingredients.
     * @param steps the ordered preparation steps.
     * @param tags tags to categorize the recipe.
     * @return the newly created [Recipe].
     */
    suspend fun create(
        name: String,
        description: String?,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
        servings: Int? = null,
    ): Recipe

    /**
     * Updates an existing recipe identified by [id].
     *
     * @param id the unique recipe identifier.
     * @param name the updated recipe name.
     * @param description the updated description.
     * @param ingredients the updated list of ingredients.
     * @param steps the updated preparation steps.
     * @param tags the updated tags.
     */
    suspend fun update(
        id: Uuid,
        name: String,
        description: String?,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
        servings: Int? = null,
    )

    /**
     * Returns a random recipe, optionally filtered by [tag] and excluding [excludeIds].
     *
     * @param tag optional tag to filter by.
     * @param excludeIds recipe IDs to exclude (for avoiding repeats).
     * @return a random [Recipe], or null if none match.
     */
    suspend fun fetchRandom(tag: String? = null, excludeIds: List<String> = emptyList()): Recipe?

    /**
     * Deletes the recipe with the given [id].
     *
     * @param id the unique recipe identifier.
     */
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
        description: String?,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
        servings: Int?,
    ): Recipe {
        return client.create(RecipeRequest(name, description, ingredients, steps, tags, servings))
    }

    override suspend fun update(
        id: Uuid,
        name: String,
        description: String?,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
        servings: Int?,
    ) {
        client.update(id, RecipeRequest(name, description, ingredients, steps, tags, servings))
    }

    override suspend fun fetchRandom(tag: String?, excludeIds: List<String>): Recipe? {
        return try {
            client.fetchRandom(tag, excludeIds)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }
    }

    override suspend fun delete(id: Uuid) {
        client.delete(id)
    }
}
