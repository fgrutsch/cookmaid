package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import kotlin.uuid.Uuid

data class ShoppingListState(
    val initialized: Boolean = false,
    val lists: List<ShoppingList> = emptyList(),
    val selectedListId: Uuid? = null,
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val suggestions: List<Item.Catalog> = emptyList(),
) {
    val selectedList: ShoppingList?
        get() = lists.find { it.id == selectedListId }

    val uncheckedItems: List<ShoppingItem>
        get() = items.filter { !it.checked }

    val checkedItems: List<ShoppingItem>
        get() = items.filter { it.checked }
}

sealed interface ShoppingListEvent {
    data object LoadLists : ShoppingListEvent
    data object Refresh : ShoppingListEvent
    data class SelectList(val listId: Uuid?) : ShoppingListEvent
    data class UpdateSearchQuery(val query: String) : ShoppingListEvent
    data object ClearSearch : ShoppingListEvent
    data class AddItem(val item: Item) : ShoppingListEvent
    data class UpdateItem(val item: ShoppingItem) : ShoppingListEvent
    data class ToggleChecked(val itemId: Uuid) : ShoppingListEvent
    data class DeleteItem(val itemId: Uuid) : ShoppingListEvent
    data object DeleteChecked : ShoppingListEvent
    data class CreateList(val name: String) : ShoppingListEvent
    data class RenameList(val listId: Uuid, val newName: String) : ShoppingListEvent
    data class DeleteList(val listId: Uuid) : ShoppingListEvent
}

sealed interface ShoppingListEffect {
    data class Error(val message: String) : ShoppingListEffect
}
