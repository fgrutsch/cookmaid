package io.github.fgrutsch.cookmaid.shopping

import io.github.fgrutsch.cookmaid.catalog.CatalogItemsTable
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategoriesTable
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

interface ShoppingListRepository {
    suspend fun findByUserId(userId: Uuid): List<ShoppingList>
    suspend fun findById(id: Uuid): ShoppingList?
    suspend fun createList(userId: Uuid, name: String, default: Boolean = false): ShoppingList
    suspend fun updateList(id: Uuid, name: String)
    suspend fun deleteList(id: Uuid)
    suspend fun findItemsByListId(listId: Uuid): List<ShoppingItem>
    suspend fun addItem(listId: Uuid, catalogItemId: Uuid?, freeTextName: String?, quantity: Float?): ShoppingItem
    suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem>
    suspend fun updateItem(itemId: Uuid, quantity: Float?, checked: Boolean)
    suspend fun deleteItem(itemId: Uuid)
    suspend fun deleteCheckedItems(listId: Uuid)
    suspend fun isListOwnedByUser(userId: Uuid, listId: Uuid): Boolean
    suspend fun isItemOwnedByUser(userId: Uuid, itemId: Uuid): Boolean
}

class PostgresShoppingListRepository : ShoppingListRepository {

    override suspend fun findByUserId(userId: Uuid): List<ShoppingList> = suspendTransaction {
        ShoppingListsTable.selectAll()
            .where(ShoppingListsTable.userId eq userId)
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

    override suspend fun createList(userId: Uuid, name: String, default: Boolean): ShoppingList = suspendTransaction {
        val row = ShoppingListsTable.insertReturning {
            it[ShoppingListsTable.userId] = userId
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
                    Item.CatalogItem(
                        id = catalogItemId,
                        name = row[CatalogItemsTable.name],
                        category = ItemCategory(
                            id = row[ItemCategoriesTable.id],
                            name = row[ItemCategoriesTable.name],
                        ),
                    )
                } else {
                    Item.FreeTextItem(name = requireNotNull(row[ShoppingItemsTable.freeTextName]))
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
            Item.CatalogItem(
                id = catalogItemId,
                name = fetched[CatalogItemsTable.name],
                category = ItemCategory(
                    id = fetched[ItemCategoriesTable.id],
                    name = fetched[ItemCategoriesTable.name],
                ),
            )
        } else {
            Item.FreeTextItem(name = requireNotNull(freeTextName))
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

    override suspend fun isListOwnedByUser(userId: Uuid, listId: Uuid): Boolean = suspendTransaction {
        ShoppingListsTable.selectAll()
            .where { (ShoppingListsTable.id eq listId) and (ShoppingListsTable.userId eq userId) }
            .count() > 0
    }

    override suspend fun isItemOwnedByUser(userId: Uuid, itemId: Uuid): Boolean = suspendTransaction {
        ShoppingItemsTable
            .join(ShoppingListsTable, JoinType.INNER, ShoppingItemsTable.listId, ShoppingListsTable.id)
            .selectAll()
            .where { (ShoppingItemsTable.id eq itemId) and (ShoppingListsTable.userId eq userId) }
            .count() > 0
    }
}

object ShoppingListsTable : Table("shopping_lists") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val name = text("name")
    val isDefault = bool("is_default").default(false)

    override val primaryKey = PrimaryKey(id)
}

object ShoppingItemsTable : Table("shopping_items") {
    val id = uuid("id").autoGenerate()
    val listId = uuid("list_id").references(ShoppingListsTable.id)
    val catalogItemId = uuid("catalog_item_id").references(CatalogItemsTable.id).nullable()
    val freeTextName = text("free_text_name").nullable()
    val quantity = float("quantity").nullable()
    val checked = bool("checked")

    override val primaryKey = PrimaryKey(id)
}
