package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.Item
import kotlinx.serialization.Serializable

@Serializable
data class RecipeIngredient(
    val item: Item,
    val quantity: String? = null,
)
