package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable

@Serializable
data class RecipeRequest(
    val name: String,
    val description: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val servings: Int? = null,
)
