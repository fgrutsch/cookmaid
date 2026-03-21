package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.user.UserId
import kotlin.uuid.Uuid

class ShoppingListService(
    private val repository: ShoppingListRepository,
) {

    suspend fun findLists(userId: UserId): List<ShoppingList> {
        return repository.find(userId)
    }

    suspend fun createList(userId: UserId, name: String): ShoppingList {
        return repository.createList(userId, name, default = false)
    }

    suspend fun updateList(userId: UserId, id: Uuid, name: String): Boolean {
        if (!repository.isListOwner(userId, id)) return false
        repository.updateList(id, name)
        return true
    }

    suspend fun deleteList(userId: UserId, id: Uuid): DeleteListResult {
        val list = repository.findById(id)
        return when {
            list == null || !repository.isListOwner(userId, id) -> DeleteListResult.NotFound
            list.default -> DeleteListResult.CannotDeleteDefault
            else -> { repository.deleteList(id); DeleteListResult.Deleted }
        }
    }

    suspend fun findItemsByListId(userId: UserId, listId: Uuid): List<ShoppingItem> {
        if (!repository.isListOwner(userId, listId)) return emptyList()
        return repository.findItemsByListId(listId)
    }

    suspend fun addItem(
        userId: UserId,
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: Float?,
    ): ShoppingItem? {
        if (!repository.isListOwner(userId, listId)) return null
        return repository.addItem(listId, catalogItemId, freeTextName, quantity)
    }

    suspend fun addItems(userId: UserId, listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>? {
        if (!repository.isListOwner(userId, listId)) return null
        return repository.addItems(listId, items)
    }

    suspend fun updateItem(userId: UserId, itemId: Uuid, quantity: Float?, checked: Boolean): Boolean {
        if (!repository.isItemOwner(userId, itemId)) return false
        repository.updateItem(itemId, quantity, checked)
        return true
    }

    suspend fun deleteItem(userId: UserId, itemId: Uuid): Boolean {
        if (!repository.isItemOwner(userId, itemId)) return false
        repository.deleteItem(itemId)
        return true
    }

    suspend fun deleteCheckedItems(userId: UserId, listId: Uuid): Boolean {
        if (!repository.isListOwner(userId, listId)) return false
        repository.deleteCheckedItems(listId)
        return true
    }
}
