package io.github.fgrutsch.catalog

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.uuid.Uuid

interface CatalogItemRepository {
    suspend fun findAll(): List<Item.CatalogItem>
    suspend fun findById(id: Uuid): Item.CatalogItem?
}

class PostgresCatalogItemRepository : CatalogItemRepository {

    private val joined = CatalogItemsTable.innerJoin(ItemCategoriesTable)

    override suspend fun findAll(): List<Item.CatalogItem> = suspendTransaction {
        joined.selectAll()
            .map { it.toCatalogItem() }
    }

    override suspend fun findById(id: Uuid): Item.CatalogItem? = suspendTransaction {
        joined.selectAll()
            .where(CatalogItemsTable.id eq id)
            .singleOrNull()
            ?.toCatalogItem()
    }

    private fun ResultRow.toCatalogItem() = Item.CatalogItem(
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
