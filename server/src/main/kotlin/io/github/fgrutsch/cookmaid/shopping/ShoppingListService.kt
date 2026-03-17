package io.github.fgrutsch.cookmaid.shopping

import kotlin.uuid.Uuid

class ShoppingListService(
    private val repository: ShoppingListRepository,
) {

    suspend fun findListsByUser(userId: Uuid): List<ShoppingList> {
        return repository.findByUserId(userId)
    }

    suspend fun createList(userId: Uuid, name: String): ShoppingList {
        return repository.createList(userId, name, default = false)
    }

    suspend fun updateList(userId: Uuid, id: Uuid, name: String): Boolean {
        if (!repository.isListOwnedByUser(userId, id)) return false
        repository.updateList(id, name)
        return true
    }

    suspend fun deleteList(userId: Uuid, id: Uuid): DeleteListResult {
        if (!repository.isListOwnedByUser(userId, id)) return DeleteListResult.NotFound
        val list = repository.findById(id) ?: return DeleteListResult.NotFound
        if (list.default) return DeleteListResult.CannotDeleteDefault
        repository.deleteList(id)
        return DeleteListResult.Deleted
    }

    enum class DeleteListResult { Deleted, NotFound, CannotDeleteDefault }

    suspend fun findItemsByListId(userId: Uuid, listId: Uuid): List<ShoppingItem> {
        if (!repository.isListOwnedByUser(userId, listId)) return emptyList()
        return repository.findItemsByListId(listId)
    }

    suspend fun addItem(userId: Uuid, listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem? {
        if (!repository.isListOwnedByUser(userId, listId)) return null
        return repository.addItem(listId, catalogItemId, freeTextName, quantity)
    }

    suspend fun addItems(userId: Uuid, listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>? {
        if (!repository.isListOwnedByUser(userId, listId)) return null
        return repository.addItems(listId, items)
    }

    suspend fun updateItem(userId: Uuid, itemId: Uuid, quantity: Float?, checked: Boolean): Boolean {
        if (!repository.isItemOwnedByUser(userId, itemId)) return false
        repository.updateItem(itemId, quantity, checked)
        return true
    }

    suspend fun deleteItem(userId: Uuid, itemId: Uuid): Boolean {
        if (!repository.isItemOwnedByUser(userId, itemId)) return false
        repository.deleteItem(itemId)
        return true
    }

    suspend fun deleteCheckedItems(userId: Uuid, listId: Uuid): Boolean {
        if (!repository.isListOwnedByUser(userId, listId)) return false
        repository.deleteCheckedItems(listId)
        return true
    }
}
