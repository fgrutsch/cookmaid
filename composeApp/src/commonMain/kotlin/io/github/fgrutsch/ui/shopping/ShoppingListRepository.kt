package io.github.fgrutsch.ui.shopping

import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingList
import kotlin.uuid.Uuid

interface ShoppingListRepository {
    val cachedLists: List<ShoppingList>
    suspend fun loadLists(): List<ShoppingList>
    suspend fun createList(name: String): ShoppingList
    suspend fun updateList(id: Uuid, name: String)
    suspend fun deleteList(id: Uuid)
    suspend fun loadItems(listId: Uuid): List<ShoppingItem>
    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem
    suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: Float?, checked: Boolean)
    suspend fun deleteItem(listId: Uuid, itemId: Uuid)
    suspend fun deleteCheckedItems(listId: Uuid)
}

class ApiShoppingListRepository(
    private val client: ShoppingListClient,
) : ShoppingListRepository {

    private var _cachedLists: List<ShoppingList> = emptyList()
    override val cachedLists: List<ShoppingList> get() = _cachedLists

    override suspend fun loadLists(): List<ShoppingList> {
        _cachedLists = client.fetchLists()
        return _cachedLists
    }

    override suspend fun createList(name: String): ShoppingList {
        val list = client.createList(name)
        _cachedLists = _cachedLists + list
        return list
    }

    override suspend fun updateList(id: Uuid, name: String) {
        client.updateList(id, name)
        _cachedLists = _cachedLists.map {
            if (it.id == id) it.copy(name = name.trim()) else it
        }
    }

    override suspend fun deleteList(id: Uuid) {
        client.deleteList(id)
        _cachedLists = _cachedLists.filter { it.id != id }
    }

    override suspend fun loadItems(listId: Uuid): List<ShoppingItem> {
        return client.fetchItems(listId)
    }

    override suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem {
        return client.addItem(listId, catalogItemId, freeTextName, quantity)
    }

    override suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: Float?, checked: Boolean) {
        client.updateItem(listId, itemId, quantity, checked)
    }

    override suspend fun deleteItem(listId: Uuid, itemId: Uuid) {
        client.deleteItem(listId, itemId)
    }

    override suspend fun deleteCheckedItems(listId: Uuid) {
        client.deleteCheckedItems(listId)
    }
}
