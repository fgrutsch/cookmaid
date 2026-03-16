package io.github.fgrutsch.cookmaid.shopping

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CreateListRequest(val name: String)

@Serializable
data class UpdateListRequest(val name: String)

@Serializable
data class CreateShoppingItemRequest(
    val catalogItemId: Uuid? = null,
    val freeTextName: String? = null,
    val quantity: Float? = null,
)

@Serializable
data class UpdateItemRequest(val quantity: Float?, val checked: Boolean)
