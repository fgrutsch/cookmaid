package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable

/**
 * Default page size for recipe pagination.
 */
const val DEFAULT_RECIPE_PAGE_SIZE = 20

@Serializable
data class RecipePage(
    val items: List<Recipe>,
    val nextCursor: String?,
)
