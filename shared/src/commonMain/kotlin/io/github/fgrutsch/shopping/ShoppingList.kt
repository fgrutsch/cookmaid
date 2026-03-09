package io.github.fgrutsch.shopping

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingList(
    val id: String,
    val name: String,
    val items: List<ShoppingItem> = emptyList(),
    val default: Boolean = false,
)
