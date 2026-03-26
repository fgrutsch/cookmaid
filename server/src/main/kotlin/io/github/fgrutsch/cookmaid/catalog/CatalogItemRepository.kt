package io.github.fgrutsch.cookmaid.catalog

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import org.jetbrains.exposed.v1.core.Column
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
     * Returns all catalog items, ordered by name in the given [locale].
     *
     * @param locale the language code (e.g. "en", "de").
     * @return the full list of catalog items.
     */
    suspend fun findAll(locale: SupportedLocale): List<Item.Catalog>

    /**
     * Returns a catalog item by [id], or null if it does not exist.
     *
     * @param id the catalog item identifier.
     * @param locale the language code (e.g. "en", "de").
     * @return the matching catalog item, or null.
     */
    suspend fun findById(id: Uuid, locale: SupportedLocale): Item.Catalog?
}

class PostgresCatalogItemRepository : CatalogItemRepository {

    private val joined = CatalogItemsTable.innerJoin(ItemCategoriesTable)

    override suspend fun findAll(locale: SupportedLocale): List<Item.Catalog> = suspendTransaction {
        val itemNameCol = resolveItemNameColumn(locale)
        joined.selectAll()
            .orderBy(itemNameCol)
            .map { it.toCatalogItem(locale) }
    }

    override suspend fun findById(id: Uuid, locale: SupportedLocale): Item.Catalog? = suspendTransaction {
        joined.selectAll()
            .where(CatalogItemsTable.id eq id)
            .singleOrNull()
            ?.toCatalogItem(locale)
    }

    private fun ResultRow.toCatalogItem(locale: SupportedLocale) = Item.Catalog(
        id = this[CatalogItemsTable.id],
        name = resolveItemName(this, locale),
        category = ItemCategory(
            id = this[ItemCategoriesTable.id],
            name = resolveCategoryName(this, locale),
        ),
    )
}

/**
 * Returns the localized catalog item name from the given [row].
 */
fun resolveItemName(row: ResultRow, locale: SupportedLocale): String =
    row[resolveItemNameColumn(locale)]

/**
 * Returns the localized category name from the given [row].
 */
fun resolveCategoryName(row: ResultRow, locale: SupportedLocale): String =
    row[resolveCategoryNameColumn(locale)]

/**
 * Returns the catalog item name column for the given [locale].
 */
fun resolveItemNameColumn(locale: SupportedLocale): Column<String> = when (locale) {
    SupportedLocale.DE -> CatalogItemsTable.nameDe
    SupportedLocale.EN -> CatalogItemsTable.nameEn
}

/**
 * Returns the category name column for the given [locale].
 */
fun resolveCategoryNameColumn(locale: SupportedLocale): Column<String> = when (locale) {
    SupportedLocale.DE -> ItemCategoriesTable.nameDe
    SupportedLocale.EN -> ItemCategoriesTable.nameEn
}

object ItemCategoriesTable : Table("item_categories") {
    val id = uuid("id")
    val nameEn = text("name_en")
    val nameDe = text("name_de")

    override val primaryKey = PrimaryKey(id)
}

object CatalogItemsTable : Table("catalog_items") {
    val id = uuid("id").autoGenerate()
    val nameEn = text("name_en")
    val nameDe = text("name_de")
    val categoryId = uuid("category_id").references(ItemCategoriesTable.id)

    override val primaryKey = PrimaryKey(id)
}
