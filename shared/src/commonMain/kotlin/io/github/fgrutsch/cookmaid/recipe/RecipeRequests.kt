package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecipeRequest(
    val name: String,
    val description: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

@Serializable
data class UpdateRecipeRequest(
    val name: String,
    val description: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)
