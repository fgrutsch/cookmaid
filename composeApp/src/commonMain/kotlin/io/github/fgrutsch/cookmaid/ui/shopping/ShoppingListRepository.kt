package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/**
 * Repository for managing shopping lists and their items.
 */
interface ShoppingListRepository {
    /**
     * Returns all shopping lists, optionally forcing a refresh from the server.
     *
     * @param refresh when true, bypasses the cache and fetches from the server.
     * @return list of [ShoppingList] entries.
     */
    suspend fun getLists(refresh: Boolean = false): List<ShoppingList>

    /**
     * Creates a new shopping list with the given [name].
     *
     * @param name the name for the new list.
     * @return the newly created [ShoppingList].
     */
    suspend fun createList(name: String): ShoppingList

    /**
     * Renames an existing shopping list.
     *
     * @param id the unique list identifier.
     * @param name the new name.
     */
    suspend fun updateList(id: Uuid, name: String)

    /**
     * Deletes the shopping list with the given [id].
     *
     * @param id the unique list identifier.
     */
    suspend fun deleteList(id: Uuid)

    /**
     * Loads all items for the given shopping list.
     *
     * @param listId the unique list identifier.
     * @return list of [ShoppingItem] entries.
     */
    suspend fun loadItems(listId: Uuid): List<ShoppingItem>

    /**
     * Adds a single item to a shopping list.
     *
     * @param listId the target list identifier.
     * @param catalogItemId optional catalog item to link.
     * @param freeTextName optional free-text name for non-catalog items.
     * @param quantity optional quantity.
     * @return the newly created [ShoppingItem].
     */
    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem

    /**
     * Adds multiple items to a shopping list in a single operation.
     *
     * @param listId the target list identifier.
     * @param items the items to add.
     * @return the newly created [ShoppingItem] entries.
     */
    suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>

    /**
     * Updates an existing shopping item.
     *
     * @param listId the list containing the item.
     * @param itemId the unique item identifier.
     * @param quantity the updated quantity.
     * @param checked whether the item is checked off.
     */
    suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: Float?, checked: Boolean)

    /**
     * Deletes a single item from a shopping list.
     *
     * @param listId the list containing the item.
     * @param itemId the unique item identifier.
     */
    suspend fun deleteItem(listId: Uuid, itemId: Uuid)

    /**
     * Deletes all checked items from the given shopping list.
     *
     * @param listId the list to clean up.
     */
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

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: Float?,
    ): ShoppingItem {
        return client.addItem(listId, catalogItemId, freeTextName, quantity)
    }

    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> {
        return client.addItems(listId, items)
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
