package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.catalog.CatalogItemsTable
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategoriesTable
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import io.github.fgrutsch.cookmaid.user.UserId
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

/**
 * Persistence layer for shopping lists and their items.
 */
interface ShoppingListRepository {
    /**
     * Returns all shopping lists for [userId], ordered with the default list first.
     *
     * @param userId the owner of the shopping lists.
     * @return the user's shopping lists.
     */
    suspend fun find(userId: UserId): List<ShoppingList>

    /**
     * Returns a shopping list by [id], or null if it does not exist.
     *
     * @param id the shopping list identifier.
     * @return the matching shopping list, or null.
     */
    suspend fun findById(id: Uuid): ShoppingList?

    /**
     * Creates a new shopping list for [userId].
     *
     * @param userId the owner of the new list.
     * @param name the display name.
     * @param default whether this is the user's default list.
     * @return the newly created shopping list.
     */
    suspend fun createList(userId: UserId, name: String, default: Boolean = false): ShoppingList

    /**
     * Renames a shopping list.
     *
     * @param id the shopping list to rename.
     * @param name the new display name.
     */
    suspend fun updateList(id: Uuid, name: String)

    /**
     * Deletes a shopping list and its items.
     *
     * @param id the shopping list to delete.
     */
    suspend fun deleteList(id: Uuid)

    /**
     * Returns all items belonging to the given shopping list.
     *
     * @param listId the shopping list whose items to retrieve.
     * @return the items in the list.
     */
    suspend fun findItemsByListId(listId: Uuid): List<ShoppingItem>

    /**
     * Adds a single item to a shopping list.
     *
     * @param listId the target shopping list.
     * @param catalogItemId optional catalog item reference.
     * @param freeTextName optional free-text item name.
     * @param quantity optional quantity.
     * @return the created shopping item.
     */
    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem

    /**
     * Adds multiple items to a shopping list in a single transaction.
     *
     * @param listId the target shopping list.
     * @param items the items to add.
     * @return the created shopping items.
     */
    suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>

    /**
     * Updates quantity and checked state of a shopping item.
     *
     * @param itemId the item to update.
     * @param quantity the new quantity.
     * @param checked the new checked state.
     */
    suspend fun updateItem(itemId: Uuid, quantity: Float?, checked: Boolean)

    /**
     * Deletes a shopping item.
     *
     * @param itemId the item to delete.
     */
    suspend fun deleteItem(itemId: Uuid)

    /**
     * Deletes all checked items from a shopping list.
     *
     * @param listId the shopping list to clear checked items from.
     */
    suspend fun deleteCheckedItems(listId: Uuid)

    /**
     * Returns true if [userId] owns the shopping list identified by [listId].
     *
     * @param userId the user to check ownership for.
     * @param listId the shopping list to check.
     * @return true if the user owns the list.
     */
    suspend fun isListOwner(userId: UserId, listId: Uuid): Boolean

    /**
     * Returns true if [userId] owns the shopping item identified by [itemId].
     *
     * @param userId the user to check ownership for.
     * @param itemId the shopping item to check.
     * @return true if the user owns the item (via its parent list).
     */
    suspend fun isItemOwner(userId: UserId, itemId: Uuid): Boolean
}

class PostgresShoppingListRepository : ShoppingListRepository {

    override suspend fun find(userId: UserId): List<ShoppingList> = suspendTransaction {
        ShoppingListsTable.selectAll()
            .where(ShoppingListsTable.userId eq userId.value)
            .orderBy(ShoppingListsTable.isDefault to SortOrder.DESC, ShoppingListsTable.name to SortOrder.ASC)
            .map { row ->
                ShoppingList(
                    id = row[ShoppingListsTable.id],
                    name = row[ShoppingListsTable.name],
                    default = row[ShoppingListsTable.isDefault],
                )
            }
    }

    override suspend fun findById(id: Uuid): ShoppingList? = suspendTransaction {
        ShoppingListsTable.selectAll()
            .where(ShoppingListsTable.id eq id)
            .singleOrNull()
            ?.let { row ->
                ShoppingList(
                    id = row[ShoppingListsTable.id],
                    name = row[ShoppingListsTable.name],
                    default = row[ShoppingListsTable.isDefault],
                )
            }
    }

    override suspend fun createList(userId: UserId, name: String, default: Boolean): ShoppingList = suspendTransaction {
        val row = ShoppingListsTable.insertReturning {
            it[ShoppingListsTable.userId] = userId.value
            it[ShoppingListsTable.name] = name.trim()
            it[ShoppingListsTable.isDefault] = default
        }.single()

        ShoppingList(
            id = row[ShoppingListsTable.id],
            name = row[ShoppingListsTable.name],
            default = row[ShoppingListsTable.isDefault],
        )
    }

    override suspend fun updateList(id: Uuid, name: String): Unit = suspendTransaction {
        ShoppingListsTable.update({ ShoppingListsTable.id eq id }) {
            it[ShoppingListsTable.name] = name.trim()
        }
    }

    override suspend fun deleteList(id: Uuid): Unit = suspendTransaction {
        ShoppingListsTable.deleteWhere { ShoppingListsTable.id eq id }
    }

    override suspend fun findItemsByListId(listId: Uuid): List<ShoppingItem> = suspendTransaction {
        val joined = ShoppingItemsTable
            .join(CatalogItemsTable, JoinType.LEFT, ShoppingItemsTable.catalogItemId, CatalogItemsTable.id)
            .join(ItemCategoriesTable, JoinType.LEFT, CatalogItemsTable.categoryId, ItemCategoriesTable.id)

        joined.selectAll()
            .where { ShoppingItemsTable.listId eq listId }
            .map { row ->
                val catalogItemId = row[ShoppingItemsTable.catalogItemId]
                val item: Item = if (catalogItemId != null) {
                    Item.Catalog(
                        id = catalogItemId,
                        name = row[CatalogItemsTable.name],
                        category = ItemCategory(
                            id = row[ItemCategoriesTable.id],
                            name = row[ItemCategoriesTable.name],
                        ),
                    )
                } else {
                    Item.FreeText(name = requireNotNull(row[ShoppingItemsTable.freeTextName]))
                }
                ShoppingItem(
                    id = row[ShoppingItemsTable.id],
                    item = item,
                    quantity = row[ShoppingItemsTable.quantity],
                    checked = row[ShoppingItemsTable.checked],
                )
            }
    }

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: Float?,
    ): ShoppingItem = suspendTransaction {
        addItemInternal(listId, catalogItemId, freeTextName, quantity)
    }

    private fun addItemInternal(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: Float?,
    ): ShoppingItem {
        val row = ShoppingItemsTable.insertReturning {
            it[ShoppingItemsTable.listId] = listId
            it[ShoppingItemsTable.catalogItemId] = catalogItemId
            it[ShoppingItemsTable.freeTextName] = freeTextName
            it[ShoppingItemsTable.quantity] = quantity
            it[ShoppingItemsTable.checked] = false
        }.single()

        val itemId = row[ShoppingItemsTable.id]

        val joined = ShoppingItemsTable
            .join(CatalogItemsTable, JoinType.LEFT, ShoppingItemsTable.catalogItemId, CatalogItemsTable.id)
            .join(ItemCategoriesTable, JoinType.LEFT, CatalogItemsTable.categoryId, ItemCategoriesTable.id)

        val fetched = joined.selectAll()
            .where { ShoppingItemsTable.id eq itemId }
            .single()

        val item: Item = if (catalogItemId != null) {
            Item.Catalog(
                id = catalogItemId,
                name = fetched[CatalogItemsTable.name],
                category = ItemCategory(
                    id = fetched[ItemCategoriesTable.id],
                    name = fetched[ItemCategoriesTable.name],
                ),
            )
        } else {
            Item.FreeText(name = requireNotNull(freeTextName))
        }

        return ShoppingItem(
            id = itemId,
            item = item,
            quantity = row[ShoppingItemsTable.quantity],
            checked = row[ShoppingItemsTable.checked],
        )
    }

    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> =
        suspendTransaction {
            items.map { req ->
                addItemInternal(listId, req.catalogItemId, req.freeTextName, req.quantity)
            }
        }

    override suspend fun updateItem(itemId: Uuid, quantity: Float?, checked: Boolean): Unit = suspendTransaction {
        ShoppingItemsTable.update({ ShoppingItemsTable.id eq itemId }) {
            it[ShoppingItemsTable.quantity] = quantity
            it[ShoppingItemsTable.checked] = checked
        }
    }

    override suspend fun deleteItem(itemId: Uuid): Unit = suspendTransaction {
        ShoppingItemsTable.deleteWhere { ShoppingItemsTable.id eq itemId }
    }

    override suspend fun deleteCheckedItems(listId: Uuid): Unit = suspendTransaction {
        ShoppingItemsTable.deleteWhere {
            (ShoppingItemsTable.listId eq listId) and (ShoppingItemsTable.checked eq true)
        }
    }

    override suspend fun isListOwner(userId: UserId, listId: Uuid): Boolean = suspendTransaction {
        ShoppingListsTable.selectAll()
            .where { (ShoppingListsTable.id eq listId) and (ShoppingListsTable.userId eq userId.value) }
            .count() > 0
    }

    override suspend fun isItemOwner(userId: UserId, itemId: Uuid): Boolean = suspendTransaction {
        ShoppingItemsTable
            .join(ShoppingListsTable, JoinType.INNER, ShoppingItemsTable.listId, ShoppingListsTable.id)
            .selectAll()
            .where { (ShoppingItemsTable.id eq itemId) and (ShoppingListsTable.userId eq userId.value) }
            .count() > 0
    }
}

object ShoppingListsTable : Table("shopping_lists") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val name = text("name")
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ShoppingItemsTable : Table("shopping_items") {
    val id = uuid("id").autoGenerate()
    val listId = uuid("list_id").references(ShoppingListsTable.id)
    val catalogItemId = uuid("catalog_item_id").references(CatalogItemsTable.id).nullable()
    val freeTextName = text("free_text_name").nullable()
    val quantity = float("quantity").nullable()
    val checked = bool("checked")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
