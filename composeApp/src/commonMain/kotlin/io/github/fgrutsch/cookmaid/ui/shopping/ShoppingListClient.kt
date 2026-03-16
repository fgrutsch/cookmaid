package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.shopping.CreateListRequest
import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.shopping.UpdateItemRequest
import io.github.fgrutsch.cookmaid.shopping.UpdateListRequest
import io.github.fgrutsch.cookmaid.ui.auth.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.Uuid

class ShoppingListClient(
    private val apiClient: ApiClient,
) {
    private val base = "/api/shopping-lists"

    suspend fun fetchLists(): List<ShoppingList> =
        apiClient.httpClient.get(base).body()

    suspend fun createList(name: String): ShoppingList =
        apiClient.httpClient.post(base) {
            contentType(ContentType.Application.Json)
            setBody(CreateListRequest(name))
        }.body()

    suspend fun updateList(id: Uuid, name: String) {
        apiClient.httpClient.put("$base/$id") {
            contentType(ContentType.Application.Json)
            setBody(UpdateListRequest(name))
        }
    }

    suspend fun deleteList(id: Uuid) {
        apiClient.httpClient.delete("$base/$id")
    }

    suspend fun fetchItems(listId: Uuid): List<ShoppingItem> =
        apiClient.httpClient.get("$base/$listId/items").body()

    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem =
        apiClient.httpClient.post("$base/$listId/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateShoppingItemRequest(catalogItemId, freeTextName, quantity))
        }.body()

    suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: Float?, checked: Boolean) {
        apiClient.httpClient.put("$base/$listId/items/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateItemRequest(quantity, checked))
        }
    }

    suspend fun deleteItem(listId: Uuid, itemId: Uuid) {
        apiClient.httpClient.delete("$base/$listId/items/$itemId")
    }

    suspend fun deleteCheckedItems(listId: Uuid) {
        apiClient.httpClient.delete("$base/$listId/items?checked=true")
    }
}
