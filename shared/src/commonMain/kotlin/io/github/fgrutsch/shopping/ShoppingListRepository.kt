package io.github.fgrutsch.shopping

import io.github.fgrutsch.catalog.DefaultCategoryIds
import io.github.fgrutsch.catalog.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ShoppingListRepository {
    val lists: StateFlow<List<ShoppingList>>
    suspend fun addList(name: String): ShoppingList
    suspend fun renameList(id: String, newName: String)
    suspend fun deleteList(id: String)
    suspend fun addItem(listId: String, item: ShoppingItem)
    suspend fun updateItem(listId: String, item: ShoppingItem)
    suspend fun toggleChecked(listId: String, itemId: String)
    suspend fun deleteItem(listId: String, itemId: String)
    suspend fun deleteChecked(listId: String)
}

class InMemoryShoppingListRepository : ShoppingListRepository {

    private val _lists = MutableStateFlow(defaultLists())
    override val lists: StateFlow<List<ShoppingList>> = _lists.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addList(name: String): ShoppingList {
        val newList = ShoppingList(id = Uuid.random().toString(), name = name.trim())
        _lists.update { it + newList }
        return newList
    }

    override suspend fun renameList(id: String, newName: String) {
        _lists.update { lists ->
            lists.map { if (it.id == id) it.copy(name = newName.trim()) else it }
        }
    }

    override suspend fun deleteList(id: String) {
        _lists.update { lists -> lists.filter { it.id != id } }
    }

    override suspend fun addItem(listId: String, item: ShoppingItem) {
        _lists.update { lists ->
            lists.map { list ->
                if (list.id == listId) list.copy(items = list.items + item)
                else list
            }
        }
    }

    override suspend fun updateItem(listId: String, item: ShoppingItem) {
        _lists.update { lists ->
            lists.map { list ->
                if (list.id == listId) {
                    list.copy(items = list.items.map { if (it.id == item.id) item else it })
                } else list
            }
        }
    }

    override suspend fun toggleChecked(listId: String, itemId: String) {
        _lists.update { lists ->
            lists.map { list ->
                if (list.id == listId) {
                    list.copy(items = list.items.map { item ->
                        if (item.id == itemId) item.copy(checked = !item.checked) else item
                    })
                } else list
            }
        }
    }

    override suspend fun deleteItem(listId: String, itemId: String) {
        _lists.update { lists ->
            lists.map { list ->
                if (list.id == listId) list.copy(items = list.items.filter { it.id != itemId })
                else list
            }
        }
    }

    override suspend fun deleteChecked(listId: String) {
        _lists.update { lists ->
            lists.map { list ->
                if (list.id == listId) list.copy(items = list.items.filter { !it.checked })
                else list
            }
        }
    }
}

private fun defaultLists(): List<ShoppingList> {
    val weekly = ShoppingList(
        id = "list-weekly",
        name = "Groceries",
        default = true,
        items = listOf(
            ShoppingItem(id = "si-1", item = Item.CategorizedItem(id = "item-milk", name = "Milk", category = DefaultCategoryIds.DAIRY), quantity = 2f),
            ShoppingItem(id = "si-2", item = Item.CategorizedItem(id = "item-bread", name = "Bread", category = DefaultCategoryIds.BAKERY), quantity = 1f),
            ShoppingItem(id = "si-3", item = Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = DefaultCategoryIds.DAIRY), quantity = 12f),
            ShoppingItem(id = "si-4", item = Item.CategorizedItem(id = "item-bananas", name = "Bananas", category = DefaultCategoryIds.FRUITS), quantity = 6f),
            ShoppingItem(id = "si-5", item = Item.CategorizedItem(id = "item-chicken", name = "Chicken", category = DefaultCategoryIds.MEAT), quantity = null),
            ShoppingItem(id = "si-6", item = Item.FreeTextItem(name = "Kitchen sponges"), quantity = 3f),
            ShoppingItem(id = "si-7", item = Item.CategorizedItem(id = "item-tomatoes", name = "Tomatoes", category = DefaultCategoryIds.VEGETABLES), quantity = 4f, checked = true),
            ShoppingItem(id = "si-8", item = Item.CategorizedItem(id = "item-butter", name = "Butter", category = DefaultCategoryIds.DAIRY), quantity = 1f, checked = true),
        ),
    )
    val party = ShoppingList(
        id = "list-party",
        name = "BBQ Party",
        items = listOf(
            ShoppingItem(id = "si-9", item = Item.CategorizedItem(id = "item-ground-beef", name = "Ground Beef", category = DefaultCategoryIds.MEAT), quantity = 2f),
            ShoppingItem(id = "si-10", item = Item.CategorizedItem(id = "item-chips", name = "Chips", category = DefaultCategoryIds.SNACKS), quantity = 3f),
            ShoppingItem(id = "si-11", item = Item.FreeTextItem(name = "Paper plates"), quantity = 20f),
        ),
    )
    return listOf(weekly, party)
}
