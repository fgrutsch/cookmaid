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
    val quantity: String? = null,
)

@Serializable
data class UpdateItemRequest(val quantity: String?, val checked: Boolean)

@Serializable
data class BatchAddItemsRequest(val items: List<CreateShoppingItemRequest>)
