package io.github.fgrutsch.shopping

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ShoppingList(
    val id: Uuid,
    val name: String,
    val default: Boolean = false,
)
