package io.github.fgrutsch.cookmaid.catalog

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.uuid.Uuid

/**
 * Persistence layer for the shared catalog of grocery items.
 */
interface CatalogItemRepository {
    /**
     * Returns all catalog items, ordered by name.
     *
     * @return the full list of catalog items.
     */
    suspend fun findAll(): List<Item.Catalog>

    /**
     * Returns a catalog item by [id], or null if it does not exist.
     *
     * @param id the catalog item identifier.
     * @return the matching catalog item, or null.
     */
    suspend fun findById(id: Uuid): Item.Catalog?
}

class PostgresCatalogItemRepository : CatalogItemRepository {

    private val joined = CatalogItemsTable.innerJoin(ItemCategoriesTable)

    override suspend fun findAll(): List<Item.Catalog> = suspendTransaction {
        joined.selectAll()
            .orderBy(CatalogItemsTable.name)
            .map { it.toCatalogItem() }
    }

    override suspend fun findById(id: Uuid): Item.Catalog? = suspendTransaction {
        joined.selectAll()
            .where(CatalogItemsTable.id eq id)
            .singleOrNull()
            ?.toCatalogItem()
    }

    private fun ResultRow.toCatalogItem() = Item.Catalog(
        id = this[CatalogItemsTable.id],
        name = this[CatalogItemsTable.name],
        category = ItemCategory(
            id = this[ItemCategoriesTable.id],
            name = this[ItemCategoriesTable.name],
        ),
    )
}

object ItemCategoriesTable : Table("item_categories") {
    val id = uuid("id")
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object CatalogItemsTable : Table("catalog_items") {
    val id = uuid("id").autoGenerate()
    val name = text("name")
    val categoryId = uuid("category_id").references(ItemCategoriesTable.id)

    override val primaryKey = PrimaryKey(id)
}
