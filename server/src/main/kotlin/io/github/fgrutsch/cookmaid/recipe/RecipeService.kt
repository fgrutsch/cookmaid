package io.github.fgrutsch.cookmaid.recipe

import kotlin.time.Instant
import kotlin.uuid.Uuid

class RecipeService(
    private val repository: RecipeRepository,
) {

    suspend fun findByUser(userId: Uuid, cursor: Instant?, limit: Int, search: String?, tag: String?): RecipePage {
        return repository.findByUserId(userId, cursor, limit, search, tag)
    }

    suspend fun findById(userId: Uuid, recipeId: Uuid): Recipe? {
        if (!repository.isOwnedByUser(userId, recipeId)) return null
        return repository.findById(recipeId)
    }

    suspend fun findTagsByUser(userId: Uuid): List<String> {
        return repository.findTagsByUserId(userId)
    }

    suspend fun create(
        userId: Uuid,
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    ): Recipe {
        return repository.create(userId, name, ingredients, steps, tags)
    }

    suspend fun update(
        userId: Uuid,
        recipeId: Uuid,
        name: String,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
    ): Boolean {
        if (!repository.isOwnedByUser(userId, recipeId)) return false
        repository.update(recipeId, name, ingredients, steps, tags)
        return true
    }

    suspend fun delete(userId: Uuid, recipeId: Uuid): Boolean {
        if (!repository.isOwnedByUser(userId, recipeId)) return false
        repository.delete(recipeId)
        return true
    }
}
