package io.github.fgrutsch.cookmaid.ui.recipe

import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import kotlin.uuid.Uuid

class FakeRecipeRepository : RecipeRepository {

    var recipes: MutableList<Recipe> = mutableListOf()
    var tags: List<String> = emptyList()
    var pageSize: Int = 20

    override suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage {
        var filtered = recipes.toList()
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter { it.name.lowercase().contains(search.lowercase()) }
        }
        if (!tag.isNullOrBlank()) {
            filtered = filtered.filter { tag in it.tags }
        }
        val startIndex = if (cursor != null) {
            filtered.indexOfFirst { it.id.toString() == cursor }.let { if (it == -1) 0 else it }
        } else {
            0
        }
        val page = filtered.drop(startIndex).take(limit)
        val hasMore = startIndex + limit < filtered.size
        val nextCursor = if (hasMore) filtered[startIndex + limit].id.toString() else null
        return RecipePage(items = page, nextCursor = nextCursor)
    }

    override suspend fun fetchTags(): List<String> = tags

    override suspend fun getById(id: Uuid): Recipe? = recipes.find { it.id == id }

    override suspend fun create(
        name: String,
        description: String?,
        ingredients: List<RecipeIngredient>,
        steps: List<String>,
        tags: List<String>,
        servings: Int?,
    ): Recipe {
        val recipe = Recipe(
            id = Uuid.random(),
            name = name,
            description = description,
            ingredients = ingredients,
            steps = steps,
            tags = tags,
        )
        recipes.add(recipe)
        return recipe
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
        recipes = recipes.map {
            if (it.id == id) {
                it.copy(name = name, description = description, ingredients = ingredients, steps = steps, tags = tags)
            } else it
        }.toMutableList()
    }

    override suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe? {
        var filtered = recipes.toList()
        if (!tag.isNullOrBlank()) {
            filtered = filtered.filter { tag in it.tags }
        }
        if (!excludeId.isNullOrBlank()) {
            filtered = filtered.filter { it.id.toString() != excludeId }
        }
        return filtered.randomOrNull()
    }

    override suspend fun delete(id: Uuid) {
        recipes.removeAll { it.id == id }
    }
}
