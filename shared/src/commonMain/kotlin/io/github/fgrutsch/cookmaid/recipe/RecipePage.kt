package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable

@Serializable
data class RecipePage(
    val items: List<Recipe>,
    val nextCursor: String?,
)
