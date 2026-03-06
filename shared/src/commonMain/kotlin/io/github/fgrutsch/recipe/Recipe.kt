package io.github.fgrutsch.recipe

import io.github.fgrutsch.catalog.Item
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String> = emptyList(),
)

@Serializable
data class RecipeIngredient(
    val item: Item,
    val quantity: Float? = null,
)
