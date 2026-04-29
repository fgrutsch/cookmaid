package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import kotlin.uuid.Uuid

class FakeShoppingListRepository : ShoppingListRepository {

    var lists: MutableList<ShoppingList> = mutableListOf()
    var itemsByList: MutableMap<Uuid, MutableList<ShoppingItem>> = mutableMapOf()
    var lastAddedCatalogItemId: Uuid? = null
    var lastAddedFreeTextName: String? = null

    override suspend fun getLists(refresh: Boolean): List<ShoppingList> = lists.toList()

    override suspend fun createList(name: String): ShoppingList {
        val list = ShoppingList(id = Uuid.random(), name = name)
        lists.add(list)
        return list
    }

    override suspend fun updateList(id: Uuid, name: String) {
        lists = lists.map { if (it.id == id) it.copy(name = name) else it }.toMutableList()
    }

    override suspend fun deleteList(id: Uuid) {
        lists.removeAll { it.id == id }
        itemsByList.remove(id)
    }

    override suspend fun loadItems(listId: Uuid): List<ShoppingItem> =
        itemsByList[listId].orEmpty()

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: String?,
    ): ShoppingItem {
        lastAddedCatalogItemId = catalogItemId
        lastAddedFreeTextName = freeTextName
        val item = ShoppingItem(
            id = Uuid.random(),
            item = if (catalogItemId != null) Item.FreeText(name = "catalog-stub") else Item.FreeText(name = freeTextName ?: "item"),
            quantity = quantity,
            checked = false,
        )
        itemsByList.getOrPut(listId) { mutableListOf() }.add(item)
        return item
    }

    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> {
        return items.map { req ->
            addItem(listId, req.catalogItemId, req.freeTextName, req.quantity)
        }
    }

    override suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: String?, checked: Boolean) {
        itemsByList[listId] = itemsByList[listId]?.map {
            if (it.id == itemId) it.copy(quantity = quantity, checked = checked) else it
        }?.toMutableList() ?: return
    }

    override suspend fun deleteItem(listId: Uuid, itemId: Uuid) {
        itemsByList[listId]?.removeAll { it.id == itemId }
    }

    override suspend fun deleteCheckedItems(listId: Uuid) {
        itemsByList[listId]?.removeAll { it.checked }
    }
}
