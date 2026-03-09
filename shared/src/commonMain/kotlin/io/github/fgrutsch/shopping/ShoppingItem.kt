package io.github.fgrutsch.shopping

import io.github.fgrutsch.catalog.Item
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(
    val id: String,
    val item: Item,
    val quantity: Float?,
    val checked: Boolean = false,
)
