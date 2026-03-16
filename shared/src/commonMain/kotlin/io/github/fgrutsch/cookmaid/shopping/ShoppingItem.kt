package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ShoppingItem(
    val id: Uuid,
    val item: Item,
    val quantity: Float?,
    val checked: Boolean = false,
)
