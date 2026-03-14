package io.github.fgrutsch.shopping

import io.github.fgrutsch.catalog.Item
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ShoppingItem(
    val id: Uuid,
    val item: Item,
    val quantity: Float?,
    val checked: Boolean = false,
)
