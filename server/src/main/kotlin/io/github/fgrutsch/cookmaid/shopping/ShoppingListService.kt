package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.uuid.Uuid

/**
 * Orchestrates shopping list and item operations with ownership checks.
 */
class ShoppingListService(
    private val repository: ShoppingListRepository,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Returns all shopping lists belonging to [userId].
     *
     * @param userId the owner of the shopping lists.
     * @return all shopping lists for the user, ordered with the default list first.
     */
    suspend fun findLists(userId: UserId): List<ShoppingList> {
        return repository.find(userId)
    }

    /**
     * Creates the default shopping list during user provisioning.
     *
     * Does not open its own transaction — the caller is responsible for atomicity.
     *
     * @param userId the owner of the new default list.
     */
    suspend fun createDefaultList(userId: UserId) {
        repository.createList(userId, "Shopping List", default = true)
        logger.info { "Default shopping list created: userId=$userId" }
    }

    /**
     * Creates a new non-default shopping list for [userId].
     *
     * @param userId the owner of the new list.
     * @param name the display name for the list.
     * @return the newly created shopping list.
     */
    suspend fun createList(userId: UserId, name: String): ShoppingList {
        val list = repository.createList(userId, name, default = false)
        logger.info { "Shopping list created: userId=$userId, listId=${list.id}" }
        return list
    }

    /**
     * Renames a shopping list if owned by [userId].
     *
     * @param userId the expected owner.
     * @param id the shopping list to rename.
     * @param name the new display name.
     * @return true if the update succeeded, false if not owned.
     */
    suspend fun updateList(userId: UserId, id: Uuid, name: String): Boolean {
        if (!repository.isListOwner(userId, id)) {
            logger.debug { "List ownership check failed: userId=$userId, listId=$id" }
            return false
        }
        repository.updateList(id, name)
        logger.info { "Shopping list updated: userId=$userId, listId=$id" }
        return true
    }

    /**
     * Deletes a shopping list, rejecting deletion of the default list.
     *
     * @param userId the expected owner.
     * @param id the shopping list to delete.
     * @return the outcome of the deletion attempt.
     */
    suspend fun deleteList(userId: UserId, id: Uuid): DeleteListResult {
        val list = repository.findById(id)
        return when {
            list == null || !repository.isListOwner(userId, id) -> {
                logger.debug { "List not found or ownership check failed: userId=$userId, listId=$id" }
                DeleteListResult.NotFound
            }
            list.default -> {
                logger.debug { "Cannot delete default list: userId=$userId, listId=$id" }
                DeleteListResult.CannotDeleteDefault
            }
            else -> {
                repository.deleteList(id)
                logger.info { "Shopping list deleted: userId=$userId, listId=$id" }
                DeleteListResult.Deleted
            }
        }
    }

    /**
     * Returns all items in the given shopping list if owned by [userId].
     *
     * @param userId the expected owner.
     * @param listId the shopping list whose items to retrieve.
     * @param locale the language code for catalog item names.
     * @return the items in the list, or null if not owned.
     */
    suspend fun findItemsByListId(userId: UserId, listId: Uuid, locale: SupportedLocale): List<ShoppingItem>? {
        if (!repository.isListOwner(userId, listId)) {
            logger.debug { "List ownership check failed: userId=$userId, listId=$listId" }
            return null
        }
        return repository.findItemsByListId(listId, locale)
    }

    /**
     * Adds a single item to a shopping list if owned by [userId].
     *
     * @param userId the expected owner.
     * @param listId the target shopping list.
     * @param catalogItemId optional catalog item reference.
     * @param freeTextName optional free-text item name (used when no catalog item).
     * @param quantity optional quantity.
     * @param locale the language code for catalog item names.
     * @return the created item, or null if the list is not owned.
     */
    suspend fun addItem(
        userId: UserId,
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: String?,
        locale: SupportedLocale,
    ): ShoppingItem? {
        if (!repository.isListOwner(userId, listId)) {
            logger.debug { "List ownership check failed: userId=$userId, listId=$listId" }
            return null
        }
        val item = repository.addItem(listId, catalogItemId, freeTextName, quantity, locale)
        logger.info { "Shopping item added: userId=$userId, listId=$listId, itemId=${item.id}" }
        return item
    }

    /**
     * Adds multiple items to a shopping list if owned by [userId].
     *
     * @param userId the expected owner.
     * @param listId the target shopping list.
     * @param items the items to add.
     * @param locale the language code for catalog item names.
     * @return the created items, or null if the list is not owned.
     */
    suspend fun addItems(
        userId: UserId,
        listId: Uuid,
        items: List<CreateShoppingItemRequest>,
        locale: SupportedLocale,
    ): List<ShoppingItem>? {
        if (!repository.isListOwner(userId, listId)) {
            logger.debug { "List ownership check failed: userId=$userId, listId=$listId" }
            return null
        }
        val created = repository.addItems(listId, items, locale)
        logger.info { "Shopping items added: userId=$userId, listId=$listId, count=${created.size}" }
        return created
    }

    /**
     * Updates quantity and checked state of a shopping item if owned by [userId].
     *
     * @param userId the expected owner.
     * @param itemId the item to update.
     * @param quantity the new quantity.
     * @param checked the new checked state.
     * @return true if the update succeeded, false if not owned.
     */
    suspend fun updateItem(userId: UserId, itemId: Uuid, quantity: String?, checked: Boolean): Boolean {
        if (!repository.isItemOwner(userId, itemId)) {
            logger.debug { "Item ownership check failed: userId=$userId, itemId=$itemId" }
            return false
        }
        repository.updateItem(itemId, quantity, checked)
        logger.info { "Shopping item updated: userId=$userId, itemId=$itemId" }
        return true
    }

    /**
     * Deletes a shopping item if owned by [userId].
     *
     * @param userId the expected owner.
     * @param itemId the item to delete.
     * @return true if the deletion succeeded, false if not owned.
     */
    suspend fun deleteItem(userId: UserId, itemId: Uuid): Boolean {
        if (!repository.isItemOwner(userId, itemId)) {
            logger.debug { "Item ownership check failed: userId=$userId, itemId=$itemId" }
            return false
        }
        repository.deleteItem(itemId)
        logger.info { "Shopping item deleted: userId=$userId, itemId=$itemId" }
        return true
    }

    /**
     * Deletes all checked items from a shopping list if owned by [userId].
     *
     * @param userId the expected owner.
     * @param listId the shopping list to clear checked items from.
     * @return true if the operation succeeded, false if not owned.
     */
    suspend fun deleteCheckedItems(userId: UserId, listId: Uuid): Boolean {
        if (!repository.isListOwner(userId, listId)) {
            logger.debug { "List ownership check failed: userId=$userId, listId=$listId" }
            return false
        }
        repository.deleteCheckedItems(listId)
        logger.info { "Checked items deleted: userId=$userId, listId=$listId" }
        return true
    }
}
