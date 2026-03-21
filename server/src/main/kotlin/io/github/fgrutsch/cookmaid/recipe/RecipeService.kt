package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.user.UserId
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RecipeService(
    private val repository: RecipeRepository,
) {

    suspend fun find(userId: UserId, cursor: Instant?, limit: Int, search: String?, tag: String?): RecipePage {
        return repository.find(userId, cursor, limit, search, tag)
    }

    suspend fun findById(userId: UserId, recipeId: Uuid): Recipe? {
        if (!repository.isOwner(userId, recipeId)) return null
        return repository.findById(recipeId)
    }

    suspend fun findTags(userId: UserId): List<String> {
        return repository.findTags(userId)
    }

    suspend fun create(userId: UserId, data: RecipeData): Recipe {
        return repository.create(userId, data)
    }

    suspend fun update(userId: UserId, recipeId: Uuid, data: RecipeData): Boolean {
        if (!repository.isOwner(userId, recipeId)) return false
        repository.update(recipeId, data)
        return true
    }

    suspend fun delete(userId: UserId, recipeId: Uuid): Boolean {
        if (!repository.isOwner(userId, recipeId)) return false
        repository.delete(recipeId)
        return true
    }
}
