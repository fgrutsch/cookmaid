package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Orchestrates recipe CRUD operations with ownership checks.
 */
class RecipeService(
    private val repository: RecipeRepository,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Returns a paginated list of recipes for [userId], optionally filtered by [search] text or [tag].
     *
     * @param userId the owner of the recipes.
     * @param cursor pagination cursor (creation timestamp) for fetching the next page.
     * @param limit maximum number of recipes to return.
     * @param search optional case-insensitive text filter on recipe name.
     * @param tag optional tag filter.
     * @param locale the language code for catalog item names.
     * @return a page of matching recipes with an optional next-page cursor.
     */
    suspend fun find(
        userId: UserId,
        cursor: Instant?,
        limit: Int,
        search: String?,
        tag: String?,
        locale: SupportedLocale,
    ): RecipePage {
        return repository.find(userId, cursor, limit, search, tag, locale)
    }

    /**
     * Returns a single recipe if owned by [userId], or null otherwise.
     *
     * @param userId the expected owner.
     * @param recipeId the recipe to look up.
     * @param locale the language code for catalog item names.
     * @return the recipe, or null if not found or not owned by [userId].
     */
    suspend fun findById(userId: UserId, recipeId: Uuid, locale: SupportedLocale): Recipe? {
        if (!repository.isOwner(userId, recipeId)) {
            logger.debug { "Ownership check failed: userId=$userId, recipeId=$recipeId" }
            return null
        }
        return repository.findById(recipeId, locale)
    }

    /**
     * Returns all distinct tags used across the user's recipes.
     *
     * @param userId the owner of the recipes.
     * @return a sorted list of unique tag strings.
     */
    suspend fun findTags(userId: UserId): List<String> {
        return repository.findTags(userId)
    }

    /**
     * Returns a random recipe for [userId], optionally filtered by [tag].
     *
     * @param userId the owner of the recipes.
     * @param tag optional tag filter.
     * @param excludeIds recipe IDs to exclude (for avoiding repeats).
     * @param locale the language code for catalog item names.
     * @return a random recipe, or null if no recipes match.
     */
    suspend fun findRandom(userId: UserId, tag: String?, excludeIds: List<Uuid>, locale: SupportedLocale): Recipe? {
        return repository.findRandom(userId, tag, excludeIds, locale)
    }

    /**
     * Creates a new recipe for [userId].
     *
     * @param userId the owner of the new recipe.
     * @param data the recipe content.
     * @param locale the language code for catalog item names.
     * @return the persisted recipe.
     */
    suspend fun create(userId: UserId, data: RecipeRequest, locale: SupportedLocale): Recipe {
        require(data.name.isNotBlank()) { "Recipe name must not be blank" }
        val recipe = repository.create(userId, data, locale)
        logger.info { "Recipe created: userId=$userId, recipeId=${recipe.id}" }
        return recipe
    }

    /**
     * Updates an existing recipe if owned by [userId].
     *
     * @param userId the expected owner.
     * @param recipeId the recipe to update.
     * @param data the new recipe content.
     * @return true if the update succeeded, false if not found or not owned.
     */
    suspend fun update(userId: UserId, recipeId: Uuid, data: RecipeRequest): Boolean {
        require(data.name.isNotBlank()) { "Recipe name must not be blank" }
        if (!repository.isOwner(userId, recipeId)) {
            logger.debug { "Ownership check failed: userId=$userId, recipeId=$recipeId" }
            return false
        }
        repository.update(recipeId, data)
        logger.info { "Recipe updated: userId=$userId, recipeId=$recipeId" }
        return true
    }

    /**
     * Deletes a recipe if owned by [userId].
     *
     * @param userId the expected owner.
     * @param recipeId the recipe to delete.
     * @return true if the deletion succeeded, false if not found or not owned.
     */
    suspend fun delete(userId: UserId, recipeId: Uuid): Boolean {
        if (!repository.isOwner(userId, recipeId)) {
            logger.debug { "Ownership check failed: userId=$userId, recipeId=$recipeId" }
            return false
        }
        repository.delete(recipeId)
        logger.info { "Recipe deleted: userId=$userId, recipeId=$recipeId" }
        return true
    }
}
