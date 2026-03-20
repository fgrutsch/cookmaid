package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Recipe(
    val id: Uuid,
    val name: String,
    val description: String? = null,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String> = emptyList(),
)
