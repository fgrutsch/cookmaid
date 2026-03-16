package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

interface ShoppingListRepository {
    suspend fun getLists(refresh: Boolean = false): List<ShoppingList>
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

    private val mutex = Mutex()
    private var cachedLists: List<ShoppingList> = emptyList()

    override suspend fun getLists(refresh: Boolean): List<ShoppingList> = mutex.withLock {
        if (refresh || cachedLists.isEmpty()) cachedLists = client.fetchLists()
        cachedLists
    }

    override suspend fun createList(name: String): ShoppingList = mutex.withLock {
        val list = client.createList(name)
        cachedLists = cachedLists + list
        list
    }

    override suspend fun updateList(id: Uuid, name: String): Unit = mutex.withLock {
        client.updateList(id, name)
        cachedLists = cachedLists.map {
            if (it.id == id) it.copy(name = name.trim()) else it
        }
    }

    override suspend fun deleteList(id: Uuid): Unit = mutex.withLock {
        client.deleteList(id)
        cachedLists = cachedLists.filter { it.id != id }
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
