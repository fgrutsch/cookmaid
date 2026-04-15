package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.shopping.BatchAddItemsRequest
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

/**
 * HTTP client for shopping-list and shopping-item endpoints.
 * Extracted as an interface so repository-level tests can substitute a fake
 * without constructing the full [ApiClient] / OIDC stack.
 */
interface ShoppingListClient {
    suspend fun fetchLists(): List<ShoppingList>
    suspend fun createList(name: String): ShoppingList
    suspend fun updateList(id: Uuid, name: String)
    suspend fun deleteList(id: Uuid)
    suspend fun fetchItems(listId: Uuid): List<ShoppingItem>
    suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>
    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: String?): ShoppingItem
    suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: String?, checked: Boolean)
    suspend fun deleteItem(listId: Uuid, itemId: Uuid)
    suspend fun deleteCheckedItems(listId: Uuid)
}

class ApiShoppingListClient(
    private val apiClient: ApiClient,
) : ShoppingListClient {
    private val base = "/api/shopping-lists"

    override suspend fun fetchLists(): List<ShoppingList> =
        apiClient.httpClient.get(base).body()

    override suspend fun createList(name: String): ShoppingList =
        apiClient.httpClient.post(base) {
            contentType(ContentType.Application.Json)
            setBody(CreateListRequest(name))
        }.body()

    override suspend fun updateList(id: Uuid, name: String) {
        apiClient.httpClient.put("$base/$id") {
            contentType(ContentType.Application.Json)
            setBody(UpdateListRequest(name))
        }
    }

    override suspend fun deleteList(id: Uuid) {
        apiClient.httpClient.delete("$base/$id")
    }

    override suspend fun fetchItems(listId: Uuid): List<ShoppingItem> =
        apiClient.httpClient.get("$base/$listId/items").body()

    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> =
        apiClient.httpClient.post("$base/$listId/items/batch") {
            contentType(ContentType.Application.Json)
            setBody(BatchAddItemsRequest(items))
        }.body()

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: String?,
    ): ShoppingItem =
        apiClient.httpClient.post("$base/$listId/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateShoppingItemRequest(catalogItemId, freeTextName, quantity))
        }.body()

    override suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: String?, checked: Boolean) {
        apiClient.httpClient.put("$base/$listId/items/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateItemRequest(quantity, checked))
        }
    }

    override suspend fun deleteItem(listId: Uuid, itemId: Uuid) {
        apiClient.httpClient.delete("$base/$listId/items/$itemId")
    }

    override suspend fun deleteCheckedItems(listId: Uuid) {
        apiClient.httpClient.delete("$base/$listId/items?checked=true")
    }
}
